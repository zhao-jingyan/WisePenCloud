package com.oriole.wisepen.resource.service.assembler;

import cn.hutool.core.bean.BeanUtil;
import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.resource.constant.ResourceConstants;
import com.oriole.wisepen.resource.domain.ComputedGroupAcl;
import com.oriole.wisepen.resource.domain.GroupTagBind;
import com.oriole.wisepen.resource.domain.MarketOfferOption;
import com.oriole.wisepen.resource.domain.base.TagInfoBase;
import com.oriole.wisepen.resource.domain.dto.res.MarketOfferInfoResponse;
import com.oriole.wisepen.resource.domain.dto.res.MarketOfferOptionResponse;
import com.oriole.wisepen.resource.domain.dto.res.ResourceItemResponse;
import com.oriole.wisepen.resource.domain.entity.ResourceItemEntity;
import com.oriole.wisepen.resource.domain.entity.TagEntity;
import com.oriole.wisepen.resource.enums.MarketOfferStatus;
import com.oriole.wisepen.resource.enums.ResourceAccessRole;
import com.oriole.wisepen.resource.enums.ResourceAction;
import com.oriole.wisepen.resource.repository.TagRepository;
import com.oriole.wisepen.user.api.domain.base.UserDisplayBase;
import com.oriole.wisepen.user.api.feign.RemoteUserService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.oriole.wisepen.resource.enums.ResourceAction.MARKET_FORBIDDEN_ACTIONS_MASK;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceItemResponseAssembler {

    private final TagRepository tagRepository;
    private final RemoteUserService remoteUserService;

    public ResourceItemResponse assembleOne(ResourceItemEntity entity, String currentUserId, Map<Long, GroupRoleType> groupRoles, List<ResourceAction> requiredResourceActions) {
        if (entity == null) return null;
        List<ResourceItemResponse> responses = assembleMany(List.of(entity), currentUserId, groupRoles, requiredResourceActions);
        return (responses != null && !responses.isEmpty()) ? responses.getFirst() : null;
    }

    public List<ResourceItemResponse> assembleMany(List<ResourceItemEntity> entities, String currentUserId, Map<Long, GroupRoleType> groupRoles, List<ResourceAction> requiredResourceActions) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        // 先处理权限过滤问题
        Map<String, List<ResourceAction>> actionsMap = new HashMap<>();
        entities = entities.stream().filter(entity->{
            List<ResourceAction> actions = ResourceAction.permissionCodeToActions(resolveAccess(entity, currentUserId, groupRoles).getActionsMask());
            actionsMap.put(entity.getResourceId(), actions);
            return new HashSet<>(actions).containsAll(requiredResourceActions);
        }).toList();

        if (entities.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, UserDisplayBase> ownerInfoMap = fetchOwnersInfo(entities);
        Map<String, List<String>> resourcesTagIdsMap = getResourcesTagIdsMap(entities, currentUserId, groupRoles);
        Map<String, TagInfoBase> tagInfosMap = getResourcesTagInfosMap(resourcesTagIdsMap.values().stream().flatMap(List::stream).collect(Collectors.toSet()));

        return entities.stream().map(entity->{
            ResourceItemResponse response = BeanUtil.copyProperties(entity, ResourceItemResponse.class);

            // 获取已解析的 CurrentActions
            response.setCurrentActions(actionsMap.get(entity.getResourceId()));
            // 解析 OwnerInfo
            response.setOwnerInfo(resolveOwnerInfo(entity, ownerInfoMap));
            // 解析 CurrentTags
            response.setCurrentTags(resolveCurrentTags(resourcesTagIdsMap.getOrDefault(entity.getResourceId(), Collections.emptyList()), tagInfosMap));
            // 解析 MarketOfferOption
            List<Map<String, MarketOfferOptionResponse>> marketOfferOptions = resolveMarketOffer(entity.getGroupBinds());

            // 仅所有者有此字段
            if (currentUserId.equals(entity.getOwnerId())) {
                // 处理权限掩码解包
                if (entity.getOverrideGrantedActionsMask() != null) {
                    Map<String, List<ResourceAction>> overrideGrantedActions = new HashMap<>();
                    entity.getOverrideGrantedActionsMask().forEach((groupId, mask) ->
                            overrideGrantedActions.put(groupId, ResourceAction.permissionCodeToActions(mask)));
                    response.setOverrideGrantedActions(overrideGrantedActions);
                }
                if (entity.getSpecifiedUsersGrantedActionsMask() != null) {
                    Map<String, List<ResourceAction>> specifiedUsersGrantedActions = new HashMap<>();
                    entity.getSpecifiedUsersGrantedActionsMask().forEach((userId, mask) -> specifiedUsersGrantedActions.put(userId, ResourceAction.permissionCodeToActions(mask)));
                    response.setSpecifiedUsersGrantedActions(specifiedUsersGrantedActions);
                }
                // 提供全部 MarketOffer 信息
                marketOfferOptions.getFirst().entrySet().stream().peek(entry -> response.getMarketOfferOptions().put(entry.getKey(), entry.getValue()));
                marketOfferOptions.getLast().entrySet().stream().peek(entry -> response.getMarketOfferOptions().put(entry.getKey(), entry.getValue()));
            } else {
                // 提供 MarketOffer 信息
                // 仅提供用户所在集市组的已上架的 MarketOffer 信息
                marketOfferOptions.getFirst().entrySet().stream().peek(entry -> {
                    if (groupRoles.keySet().contains(entry.getKey())) {
                        response.getMarketOfferOptions().put(entry.getKey(), entry.getValue());
                    }
                });
                // 提供用户所在集市组的未上架的 MarketOffer 信息（当用户是集市组的管理员时）
                marketOfferOptions.getLast().entrySet().stream().peek(entry -> {
                    if (groupRoles.get(entry.getKey()).equals(GroupRoleType.ADMIN) || groupRoles.get(entry.getKey()).equals(GroupRoleType.OWNER)) {
                        response.getMarketOfferOptions().put(entry.getKey(), entry.getValue());
                    }
                });
            }
            return response;
        }).toList();
    }

    // 远程批量请求所有者信息
    private Map<Long, UserDisplayBase> fetchOwnersInfo(List<ResourceItemEntity> entities) {
        List<Long> ownerIds = entities.stream()
                .map(ResourceItemEntity::getOwnerId)
                .filter(StringUtils::hasText)
                .map(Long::valueOf).distinct().toList();
        try {
            Map<Long, UserDisplayBase> fetched = remoteUserService.getUserDisplayInfo(ownerIds).getData();
            return fetched == null ? Collections.emptyMap() : fetched;
        } catch (Exception e) {
            log.warn("owner info batch degraded. ownerCount={}", ownerIds.size(), e);
            return Collections.emptyMap();
        }
    }

    // 获取有权访问的 resourcesTagIdsMap
    private Map<String, List<String>> getResourcesTagIdsMap(List<ResourceItemEntity> entities,
                                                       String currentUserId,
                                                       Map<Long, GroupRoleType> groupRoles) {
        Map<String, List<String>> resourcesTagIdsMap = new HashMap<>();
        // 收集用户有权访问的组 (包括个人组)
        Set<String> accessibleGroupIds = groupRoles == null ? new HashSet<>() : groupRoles.keySet().stream().map(String::valueOf).collect(Collectors.toSet());
        accessibleGroupIds.add(ResourceConstants.PERSONAL_GROUP_PREFIX + currentUserId);

        for (ResourceItemEntity entity : entities) {
            // 收集资源绑定的、在用户有权访问的组中的标签
            List<String> tagIds = entity.getGroupBinds() == null ? Collections.emptyList() :
                    entity.getGroupBinds().stream()
                    .filter(bind -> accessibleGroupIds.contains(bind.getGroupId()))
                    .map(GroupTagBind::getTagIds)
                    .filter(Objects::nonNull).flatMap(List::stream).distinct().toList();
            resourcesTagIdsMap.put(entity.getResourceId(), tagIds);
        }
        return resourcesTagIdsMap;
    }

    // 批量获取标签名
    private Map<String, TagInfoBase> getResourcesTagInfosMap(Set<String> allTagIds) {
        if (allTagIds.isEmpty()) return Collections.emptyMap();
        Map<String, TagInfoBase> tagNamesMap = new HashMap<>();
        Iterable<TagEntity> tagEntities = tagRepository.findAllById(allTagIds);
        for (TagEntity tagEntity : tagEntities) {
            tagNamesMap.put(tagEntity.getTagId(), BeanUtil.copyProperties(tagEntity, TagInfoBase.class));
        }
        return tagNamesMap;
    }


    @Getter
    @AllArgsConstructor
    public static class ResolvedResourceAccess {
        private final ResourceAccessRole resourceAccessRole;
        private final Set<String> permissionSources;
        private final int actionsMask;
    }

    // 预计算 ACL 快速鉴权 (拦截非法越权访问)
    public ResolvedResourceAccess resolveAccess(ResourceItemEntity entity,
                                                String currentUserId,
                                                Map<Long, GroupRoleType> groupRoles) {
        // 资源所有者有全部权限
        if (currentUserId.equals(entity.getOwnerId())) {
            return new ResolvedResourceAccess(ResourceAccessRole.OWNER, Collections.emptySet(), ResourceAction.ALL_ACTIONS);
        }

        // 检查资源级的“指定用户特权”
        Integer specifiedMask = entity.getSpecifiedUsersGrantedActionsMask() == null ? null : entity.getSpecifiedUsersGrantedActionsMask().get(currentUserId);
        if (specifiedMask != null) {
            return new ResolvedResourceAccess(ResourceAccessRole.OWNER_SPECIFIED, Collections.emptySet(), specifiedMask);
        }

        // 判断是否缺乏群组上下文（用户不在任何组 或 资源不在任何组）
        if ((groupRoles == null || groupRoles.isEmpty()) && (entity.getGroupBinds() == null || entity.getGroupBinds().isEmpty())){
            // 如果没有群组上下文，也没有被单独赋予“指定用户特权”，直接拒绝
            return new ResolvedResourceAccess(ResourceAccessRole.NONE, Collections.emptySet(), 0);
        }

        Set<String> permissionSources = new HashSet<>();
        int groupActionsMask = 0;
        ResourceAccessRole groupResourceAccessRole = ResourceAccessRole.NONE;
        // 计算群组权限
        if (entity.getGroupBinds() != null && entity.getComputedGroupAcls() != null && groupRoles != null) {
            for (Map.Entry<String, ComputedGroupAcl> entry : entity.getComputedGroupAcls().entrySet()) { // 遍历预计算的群组 ACL
                Long groupId = Long.valueOf(entry.getKey());
                if (!groupRoles.containsKey(groupId)) continue; // 用户不在该组，跳过

                GroupRoleType groupRole = groupRoles.get(groupId);

                // 用户是组管理员/拥有者，有全部权限
                if (groupRole == GroupRoleType.ADMIN || groupRole == GroupRoleType.OWNER) {
                    boolean isMarketGroup = entity.getGroupBinds().stream().anyMatch(groupTagBind ->
                            groupTagBind.getGroupId().equals(groupId.toString()) && groupTagBind.getMarketOffer() != null);
                    if (isMarketGroup) {
                        // 不能存在 MARKET_FORBIDDEN_ACTIONS_MASK 中的权限
                        permissionSources.add(groupId.toString());
                        groupActionsMask = ResourceAction.ALL_ACTIONS & ~MARKET_FORBIDDEN_ACTIONS_MASK;
                        groupResourceAccessRole = ResourceAccessRole.GROUP_ADMIN;
                    } else {
                        permissionSources.add(groupId.toString());
                        groupActionsMask = ResourceAction.ALL_ACTIONS;
                        groupResourceAccessRole = ResourceAccessRole.GROUP_ADMIN;
                    }
                    break;
                }

                // 提取预计算ACL
                ComputedGroupAcl acl = entry.getValue();
                Integer resolvedGroupMask = acl.getUserMasks().getOrDefault(currentUserId, acl.getBaseMask());

                // 只要有一个组能下发权限（无权限(0)不在此列），基础身份就是 GROUP_MEMBER
                if (resolvedGroupMask != 0) {
                    permissionSources.add(groupId.toString());
                    // 累加普通成员在不同小组下获得的权限 (按位或)
                    groupActionsMask |= resolvedGroupMask;
                    groupResourceAccessRole = ResourceAccessRole.GROUP_MEMBER;
                }
            }
        }

        return new ResolvedResourceAccess(groupResourceAccessRole, permissionSources, groupActionsMask);
    }

    private UserDisplayBase resolveOwnerInfo(ResourceItemEntity entity, Map<Long, UserDisplayBase> ownerInfoMap) {
        Long owner = Long.valueOf(entity.getOwnerId());
        UserDisplayBase ownerInfo = ownerInfoMap.get(owner);
        return ownerInfo == null ? new UserDisplayBase("UNKNOW", null, null, null) : ownerInfo;
    }

    private Map<String, TagInfoBase> resolveCurrentTags(List<String> tagIds, Map<String, TagInfoBase> tagNameMap) {
        Map<String, TagInfoBase> currentTags = new LinkedHashMap<>();
        for (String tagId : tagIds) {
            currentTags.put(tagId, tagNameMap.getOrDefault(tagId, TagInfoBase.builder().tagName("UNKNOW").build()));
        }
        return currentTags;
    }

    private List<Map<String, MarketOfferOptionResponse>> resolveMarketOffer(List<GroupTagBind> groupBinds) {
        Map<String, MarketOfferOptionResponse> onShelf = new HashMap<>();
        Map<String, MarketOfferOptionResponse> notOnShelf = new HashMap<>();

        groupBinds.stream().peek(bind -> {
            if (bind.getMarketOffer() != null) {
                MarketOfferOption offer = bind.getMarketOffer();
                // MarketOfferOption 转换为 MarketOfferOptionResponse
                MarketOfferOptionResponse marketOfferOptionResponse = BeanUtil.copyProperties(offer, MarketOfferOptionResponse.class);
                marketOfferOptionResponse.setMarketOfferList(offer.getMarketOfferList().stream().map(marketOfferInfoBase -> {
                    MarketOfferInfoResponse marketOfferInfoResponse = BeanUtil.copyProperties(marketOfferInfoBase, MarketOfferInfoResponse.class);
                    marketOfferInfoResponse.setGrantedActions(ResourceAction.permissionCodeToActions(marketOfferInfoBase.getGrantedActionsMask()));
                    return marketOfferInfoResponse;
                }).toList());
                // 拆分是否已经上架
                if (offer.getStatus().equals(MarketOfferStatus.PUBLISHED)) {
                    onShelf.put(bind.getGroupId(), marketOfferOptionResponse);
                } else {
                    notOnShelf.put(bind.getGroupId(), marketOfferOptionResponse);
                }
            }
        });
        return List.of(onShelf, notOnShelf);
    }
}

