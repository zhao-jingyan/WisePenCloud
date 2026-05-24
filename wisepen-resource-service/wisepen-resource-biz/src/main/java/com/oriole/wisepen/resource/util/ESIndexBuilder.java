package com.oriole.wisepen.resource.util;

import com.oriole.wisepen.resource.constant.ResourceConstants;
import com.oriole.wisepen.resource.domain.ComputedGroupAcl;
import com.oriole.wisepen.resource.domain.GroupTagBind;
import com.oriole.wisepen.resource.domain.entity.ESIndexEntity;
import com.oriole.wisepen.resource.domain.entity.ResourceItemEntity;
import com.oriole.wisepen.resource.domain.entity.TagEntity;
import com.oriole.wisepen.resource.enums.ResourceAction;
import com.oriole.wisepen.resource.enums.ResourceType;
import com.oriole.wisepen.resource.repository.ResourceItemRepository;
import com.oriole.wisepen.resource.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * ES 索引构建器。
 * <p>
 * 从 Mongo 读出最新的 {@link ResourceItemEntity}，把其中的 ACL 字段
 * <strong>按 DISCOVER 位投影</strong>到 {@link ESIndexEntity}（投影规则见 FULL_TEXT_SEARCH.md §2.4 + §5.1）。
 * <p>
 * 投影是一次性、无状态、纯函数式的：输入完整 Integer mask，输出 boolean / userId 集合。
 * 这一步是搜索域里 <strong>唯一</strong> 对 ACL 做的处理 —— 没有重算、没有标签溯源、没有白黑名单解析
 * （这些早就在资源域 ACL 重算阶段烧进 {@code computedGroupAcls} 了）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ESIndexBuilder {

    private final ResourceItemRepository resourceItemRepository;
    private final TagRepository tagRepository;

    /**
     * 根据 resourceId 反查最新 {@link ResourceItemEntity}，并组装一份完整的 {@link ESIndexEntity}。
     *
     * @param resourceId 资源 ID
     * @param rawContent 上游传过来的正文；可为 {@code null}（仅元数据同步时）
     * @return 资源不存在时返回 {@link Optional#empty()}，让上层跳过本次 Upsert
     */
    public Optional<ESIndexEntity> build(String resourceId, String rawContent) {
        ResourceItemEntity entity = resourceItemRepository.findById(resourceId).orElse(null);
        if (entity == null) {
            log.warn("ES index build skipped: resource not found resourceId={}", resourceId);
            return Optional.empty();
        }
        return Optional.of(build(entity, rawContent));
    }

    /**
     * 直接从已加载的 {@link ResourceItemEntity} 组装 ES 文档（公开给批量初始化任务使用）。
     */
    public ESIndexEntity build(ResourceItemEntity entity, String rawContent) {
        ResourceType resourceType = entity.getResourceType() == null ? ResourceType.UNKNOWN : entity.getResourceType();

        ESIndexEntity esEntity = ESIndexEntity.builder()
                .id(ESIndexEntity.generateEsId(resourceType, entity.getResourceId()))
                .resourceId(entity.getResourceId())
                .resourceType(resourceType.getExtension())
                .resourceName(safeEscape(entity.getResourceName()))
                .content(safeEscape(rawContent))
                .tags(collectTagNames(entity))
                .ownerId(entity.getOwnerId())
                .specifiedDiscoverUsers(projectSpecifiedDiscoverUsers(entity.getSpecifiedUsersGrantedActionsMask()))
                .computedGroupAcls(projectComputedGroupAcls(entity.getComputedGroupAcls()))
                .updateTime(entity.getUpdateTime())
                .build();

        log.debug("ES index built resourceId={} esId={} groupAclCount={} specifiedDiscoverCount={}",
                entity.getResourceId(),
                esEntity.getId(),
                esEntity.getComputedGroupAcls() == null ? 0 : esEntity.getComputedGroupAcls().size(),
                esEntity.getSpecifiedDiscoverUsers() == null ? 0 : esEntity.getSpecifiedDiscoverUsers().size());
        return esEntity;
    }

    // ============== 内部：DISCOVER 位投影 ==============

    /**
     * {@code specifiedUsersGrantedActionsMask: Map<uid, Integer>} →
     * {@code specifiedDiscoverUsers: List<uid>}，仅保留 DISCOVER 位为 1 的 uid（§2.2 Case B）。
     */
    private List<String> projectSpecifiedDiscoverUsers(Map<String, Integer> specifiedUsersGrantedActionsMask) {
        if (specifiedUsersGrantedActionsMask == null || specifiedUsersGrantedActionsMask.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(specifiedUsersGrantedActionsMask.size());
        for (Map.Entry<String, Integer> e : specifiedUsersGrantedActionsMask.entrySet()) {
            if (hasDiscoverBit(e.getValue())) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    /**
     * {@code computedGroupAcls: Map<groupId, ComputedGroupAcl>} → List of 投影后的嵌套结构。
     * <ul>
     *   <li>{@code baseMask} → {@code isDiscover: boolean}（该组默认是否可见）</li>
     *   <li>{@code userMasks} → 折叠成单一 {@code specifiedUsers}：仅保留 DISCOVER 位与
     *       {@code isDiscover} <strong>相反</strong>的 userId。与组默认一致的项是冗余信息，直接丢弃。
     *       因此 {@code specifiedUsers} 在 {@code isDiscover=true} 时是黑名单，在 {@code isDiscover=false} 时是白名单 ——
     *       两者永远不同时存在。</li>
     *   <li>个人空间组（{@code groupId} 以 {@link ResourceConstants#PERSONAL_GROUP_PREFIX} 开头）<strong>不写入</strong>，
     *       由 ownerId 兜底</li>
     * </ul>
     */
    private List<ESIndexEntity.ComputedGroupAclProjection> projectComputedGroupAcls(
            Map<String, ComputedGroupAcl> computedGroupAcls) {
        if (computedGroupAcls == null || computedGroupAcls.isEmpty()) {
            return Collections.emptyList();
        }
        List<ESIndexEntity.ComputedGroupAclProjection> result = new ArrayList<>(computedGroupAcls.size());
        for (Map.Entry<String, ComputedGroupAcl> entry : computedGroupAcls.entrySet()) {
            String groupId = entry.getKey();
            if (groupId == null || groupId.startsWith(ResourceConstants.PERSONAL_GROUP_PREFIX)) {
                continue;
            }
            ComputedGroupAcl acl = entry.getValue();
            if (acl == null) {
                continue;
            }
            boolean isDiscover = hasDiscoverBit(acl.getBaseMask());

            List<String> specifiedUsers = new ArrayList<>();
            if (acl.getUserMasks() != null) {
                for (Map.Entry<String, Integer> u : acl.getUserMasks().entrySet()) {
                    // 只收录与组默认行为相反的用户：行为一致即冗余，丢弃可减小索引体积并简化查询
                    if (hasDiscoverBit(u.getValue()) != isDiscover) {
                        specifiedUsers.add(u.getKey());
                    }
                }
            }
            result.add(ESIndexEntity.ComputedGroupAclProjection.builder()
                    .groupId(groupId)
                    .isDiscover(isDiscover)
                    .specifiedUsers(specifiedUsers)
                    .build());
        }
        return result;
    }

    /**
     * 翻译 tagId → tagName，让 ES 端按 tagName 做高亮/检索更友好。
     * <p>
     * 遍历资源所有 {@link GroupTagBind}，去重收集 tagId 后批量查 {@link TagRepository}。
     */
    private List<String> collectTagNames(ResourceItemEntity entity) {
        if (entity.getGroupBinds() == null || entity.getGroupBinds().isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> tagIds = new HashSet<>();
        for (GroupTagBind bind : entity.getGroupBinds()) {
            if (bind.getTagIds() != null) {
                tagIds.addAll(bind.getTagIds());
            }
        }
        if (tagIds.isEmpty()) {
            return Collections.emptyList();
        }
        Iterable<TagEntity> tagEntities = tagRepository.findAllById(tagIds);
        List<String> tagNames = new ArrayList<>();
        for (TagEntity tag : tagEntities) {
            if (tag.getTagName() != null && !tag.getTagName().isBlank()) {
                tagNames.add(tag.getTagName());
            }
        }
        return tagNames;
    }

    /** 复用 {@link ResourceAction#hasAction}，对 {@code null} mask 兼容（null → false） */
    private boolean hasDiscoverBit(Integer mask) {
        return mask != null && ResourceAction.hasAction(mask, ResourceAction.DISCOVER);
    }

    /** 写入 ES 前对 resourceName / content 做 HTML escape，避免高亮渲染时的 XSS 风险 */
    private String safeEscape(String text) {
        if (text == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(text);
    }
}
