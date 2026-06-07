package com.oriole.wisepen.resource.service.impl;

import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.resource.domain.dto.req.GroupResConfigUpdateRequest;
import com.oriole.wisepen.resource.domain.dto.res.GroupResConfigResponse;
import com.oriole.wisepen.resource.domain.entity.GroupResConfigEntity;
import com.oriole.wisepen.resource.domain.entity.ResourceItemEntity;
import com.oriole.wisepen.resource.enums.FileOrganizationLogic;
import com.oriole.wisepen.resource.enums.ResourceAction;
import com.oriole.wisepen.resource.exception.ResourceError;
import com.oriole.wisepen.resource.repository.GroupResConfigRepository;
import com.oriole.wisepen.resource.repository.ResourceItemRepository;
import com.oriole.wisepen.resource.mq.IResourceEventPublisher;
import com.oriole.wisepen.resource.service.IGroupResService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.oriole.wisepen.resource.constant.ResourceConstants.CONFIG_TRASH_COLLECTION;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupResServiceImpl implements IGroupResService {

    private final GroupResConfigRepository groupResConfigRepository;
    private final ResourceItemRepository resourceItemRepository;
    private final IResourceEventPublisher aclEventPublisher;
    private final MongoTemplate mongoTemplate;

    @Override
    public GroupResConfigResponse getGroupResConfig(String groupId) {
        GroupResConfigEntity entity = groupResConfigRepository.findByGroupId(groupId).orElse(null);

        FileOrganizationLogic fileOrgLogic = entity != null ? entity.getFileOrgLogic() : FileOrganizationLogic.FOLDER;
        List<ResourceAction> defaultMemberActions = ResourceAction.permissionCodeToActions(entity != null ? entity.getDefaultMemberActionsMask() : ResourceAction.DEFAULT_MEMBER_ACTIONS);

        return new GroupResConfigResponse(groupId, fileOrgLogic, defaultMemberActions);
    }

    @Override
    public void upsertGroupResConfig(GroupResConfigUpdateRequest req) {
        GroupResConfigEntity entity = groupResConfigRepository.findByGroupId(req.getGroupId())
                .orElseGet(() -> {
                    GroupResConfigEntity newEntity = new GroupResConfigEntity();
                    newEntity.setGroupId(req.getGroupId());
                    newEntity.setDefaultMemberActionsMask(ResourceAction.DEFAULT_MEMBER_ACTIONS);
                    return newEntity;
                });
        boolean shouldRecalculate = false;

        if (req.getFileOrgLogic() != null) {
            // 禁止文件管理模式从TAG切换至FOLDER
            if (entity.getFileOrgLogic() == FileOrganizationLogic.TAG && req.getFileOrgLogic() == FileOrganizationLogic.FOLDER) {
                throw new ServiceException(ResourceError.CANNOT_CHANGE_FILE_ORG_LOGIC_FROM_TAG_TO_FOLDER);
            }
            entity.setFileOrgLogic(req.getFileOrgLogic());
        }

        if (req.getDefaultMemberActions() != null) {
            Integer newMask = ResourceAction.actionsToPermissionCode(req.getDefaultMemberActions());
            if (!Objects.equals(entity.getDefaultMemberActionsMask(), newMask)) {
                entity.setDefaultMemberActionsMask(newMask);
                shouldRecalculate = true;
            }
        }

        groupResConfigRepository.save(entity);

        // 兜底掩码发生实质性变化时，触发该组所有资源的重算
        List<ResourceItemEntity> affectedResources = shouldRecalculate
                ? resourceItemRepository.findByGroupId(req.getGroupId())
                : Collections.emptyList();
        int affectedCount = affectedResources == null ? 0 : affectedResources.size();
        log.info("group resource config upserted. groupId={} fileOrgLogic={} defaultMask={} defaultMaskChanged={} affectedResources={}",
                req.getGroupId(), entity.getFileOrgLogic(), entity.getDefaultMemberActionsMask(),
                shouldRecalculate, affectedCount);

        if (affectedCount > 0) {
            for (ResourceItemEntity resource : affectedResources) {
                aclEventPublisher.publishAclRecalculateEvent(resource.getResourceId(), "GROUP_DEFAULT_MASK_CHANGED");
            }
        }
    }

    @Override
    public FileOrganizationLogic getFileOrgLogic(String groupId) {
        return groupResConfigRepository.findByGroupId(groupId)
                .map(GroupResConfigEntity::getFileOrgLogic)
                .orElse(FileOrganizationLogic.FOLDER);
    }

    @Override
    public void softRemoveGroupResConfigByGroupId(String groupId) {
        // 配置软删除 将 dissolvedAt 记录后移入 TRASH_COLLECTION
        GroupResConfigEntity config = groupResConfigRepository.findByGroupId(groupId)
                .orElseGet(() -> {
                    GroupResConfigEntity newEntity = new GroupResConfigEntity();
                    newEntity.setGroupId(groupId);
                    return newEntity;
                });
        config.setDissolvedAt(LocalDateTime.now());
        mongoTemplate.save(config, CONFIG_TRASH_COLLECTION);
        groupResConfigRepository.deleteByGroupId(groupId);
        log.info("group resource config deleted. mode=soft groupId={}", groupId);
    }

    @Override
    public void hardRemoveGroupResConfigByGroupId(String groupId) {
        mongoTemplate.remove(
                Query.query(Criteria.where("groupId").is(groupId)),
                CONFIG_TRASH_COLLECTION
        );
        log.info("group resource config deleted. mode=hard groupId={}", groupId);
    }
}
