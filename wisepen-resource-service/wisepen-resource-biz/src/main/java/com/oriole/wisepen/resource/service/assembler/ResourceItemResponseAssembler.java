package com.oriole.wisepen.resource.service.assembler;

import cn.hutool.core.bean.BeanUtil;
import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.resource.constant.ResourceConstants;
import com.oriole.wisepen.resource.domain.ComputedGroupAcl;
import com.oriole.wisepen.resource.domain.GroupTagBind;
import com.oriole.wisepen.resource.domain.MarketOfferInfo;
import com.oriole.wisepen.resource.domain.MarketOfferOptions;
import com.oriole.wisepen.resource.domain.base.TagInfoBase;
import com.oriole.wisepen.resource.domain.dto.res.MarketOfferInfoResponse;
import com.oriole.wisepen.resource.domain.dto.res.MarketOfferOptionsResponse;
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
        return assembleMany(entities, currentUserId, groupRoles, requiredResourceActions, null);
    }

    public List<ResourceItemResponse> assembleMany(List<ResourceItemEntity> entities, String currentUserId, Map<Long, GroupRoleType> groupRoles,
                                                   List<ResourceAction> requiredResourceActions, String visibleMarketGroupId) {
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
            // 仅在明确的集市组上下文下解析上架权益
            response.setMarketOffers(resolveVisibleMarketOffers(entity, visibleMarketGroupId, currentUserId, groupRoles));

            // 仅所有者有此字段
            if (currentUserId.equals(entity.getOwnerId())) {
                // 处理权限掩码解包
                if (entity.getOverrideGrantedActionsMask() != null) {
                    response.setOverrideGrantedActions(ResourceAction.permissionCodeToActions(entity.getOverrideGrantedActionsMask()));
                }
                if (entity.getSpecifiedUsersGrantedActionsMask() != null) {
                    Map<String, List<ResourceAction>> specifiedUsersGrantedActions = new HashMap<>();
                    entity.getSpecifiedUsersGrantedActionsMask().forEach((userId, mask) -> specifiedUsersGrantedActions.put(userId, ResourceAction.permissionCodeToActions(mask)));
                    response.setSpecifiedUsersGrantedActions(specifiedUsersGrantedActions);
                }
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
                    permissionSources.add(groupId.toString());
                    groupActionsMask = ResourceAction.ALL_ACTIONS;
                    groupResourceAccessRole = ResourceAccessRole.GROUP_ADMIN;
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

        // 应用资源群组策略覆盖
        // 优先级：群组策略覆盖 (Override) > 群组策略 (actionsMask)
        if (groupResourceAccessRole != ResourceAccessRole.NONE) { // 组策略覆盖的前提是用户在某个组内且有基础权限（无权限(0)不在此列）
            groupActionsMask = entity.getOverrideGrantedActionsMask() == null ? groupActionsMask : entity.getOverrideGrantedActionsMask();
        }
        return new ResolvedResourceAccess(groupResourceAccessRole, permissionSources, groupActionsMask);
    }

    private MarketOfferOptionsResponse resolveVisibleMarketOffers(ResourceItemEntity entity, String groupId,
                                                                  String currentUserId, Map<Long, GroupRoleType> groupRoles) {
        if (!StringUtils.hasText(groupId) || entity.getGroupBinds() == null) {
            return null;
        }
        GroupTagBind marketBind = entity.getGroupBinds().stream()
                .filter(bind -> groupId.equals(bind.getGroupId()))
                .findFirst()
                .orElse(null);
        if (marketBind == null || marketBind.getMarketOffers() == null) {
            return null;
        }

        boolean isOwner = currentUserId.equals(entity.getOwnerId());
        GroupRoleType groupRole = null;
        if (groupRoles != null && !groupId.startsWith(ResourceConstants.PERSONAL_GROUP_PREFIX)) {
            groupRole = groupRoles.get(Long.valueOf(groupId));
        }
        boolean isGroupAdmin = groupRole == GroupRoleType.ADMIN || groupRole == GroupRoleType.OWNER;
        MarketOfferOptions offers = marketBind.getMarketOffers();
        MarketOfferInfoResponse forkOnce = null;
        MarketOfferInfo forkOnceOffer = offers.getForkOnce();
        if (forkOnceOffer != null && (isOwner || isGroupAdmin || forkOnceOffer.getStatus() == MarketOfferStatus.PUBLISHED)) {
            forkOnce = BeanUtil.copyProperties(forkOnceOffer, MarketOfferInfoResponse.class);
        }
        MarketOfferInfoResponse forkUnlimited = null;
        MarketOfferInfo forkUnlimitedOffer = offers.getForkUnlimited();
        if (forkUnlimitedOffer != null && (isOwner || isGroupAdmin || forkUnlimitedOffer.getStatus() == MarketOfferStatus.PUBLISHED)) {
            forkUnlimited = BeanUtil.copyProperties(forkUnlimitedOffer, MarketOfferInfoResponse.class);
        }
        if (forkOnce == null && forkUnlimited == null) {
            return null;
        }
        MarketOfferOptionsResponse response = new MarketOfferOptionsResponse();
        response.setForkOnce(forkOnce);
        response.setForkUnlimited(forkUnlimited);
        return response;
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
}
