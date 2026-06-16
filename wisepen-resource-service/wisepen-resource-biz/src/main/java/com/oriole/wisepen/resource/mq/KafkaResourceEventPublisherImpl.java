package com.oriole.wisepen.resource.mq;

import com.oriole.wisepen.common.mq.ReliablePublisher;
import com.oriole.wisepen.resource.constant.MqTopicConstants;
import com.oriole.wisepen.resource.domain.entity.ResourceItemEntity;
import com.oriole.wisepen.resource.domain.mq.AclRecalculateMessage;
import com.oriole.wisepen.resource.domain.mq.ResourceDeletedMessage;
import com.oriole.wisepen.resource.domain.mq.ResourceForkMessage;
import com.oriole.wisepen.resource.enums.ResourceType;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.oriole.wisepen.resource.constant.MqTopicConstants.TOPIC_ACL_RECALC;

@Slf4j
@Component // 或者 @Service
@RequiredArgsConstructor
public class KafkaResourceEventPublisherImpl implements IResourceEventPublisher {

    @Resource
    ReliablePublisher reliablePublisher;

    @Override
    public void publishAclRecalculateEvent(String resourceId, String triggerSource) {
        AclRecalculateMessage msg = AclRecalculateMessage.builder()
                .resourceId(resourceId)
                .triggerSource(triggerSource)
                .build();
        reliablePublisher.publish(TOPIC_ACL_RECALC, resourceId, msg, resourceId);
        log.debug("acl recalculation event publish requested. topic={} resourceId={} trigger={}",
                TOPIC_ACL_RECALC, resourceId, triggerSource);
    }

    @Override
    public void publishResDeletedEvent(List<ResourceItemEntity> resourceList) {
        if (resourceList == null || resourceList.isEmpty()) {
            return;
        }
        List<String> resourceIds = resourceList.stream()
                .map(ResourceItemEntity::getResourceId).collect(Collectors.toList());
        Map<ResourceType, List<String>> typedResourceIds = resourceList.stream()
                .collect(Collectors.groupingBy(
                        entity -> entity.getResourceType() != null ? entity.getResourceType() : ResourceType.UNKNOWN,
                        Collectors.mapping(ResourceItemEntity::getResourceId, Collectors.toList())
                ));
        ResourceDeletedMessage message = ResourceDeletedMessage.builder().typedResourceIds(typedResourceIds).build();
        String dedupKey = Integer.toHexString(resourceIds.stream().sorted().collect(Collectors.joining(",")).hashCode());
        int resourceCount = resourceIds.size();
        reliablePublisher.publish(MqTopicConstants.TOPIC_RESOURCE_PHYSICAL_DESTROY, dedupKey, message, dedupKey);
        log.info("resource physical destroy event publish requested. topic={} resourceCount={} dedupKey={}",
                MqTopicConstants.TOPIC_RESOURCE_PHYSICAL_DESTROY, resourceCount, dedupKey);
    }

    @Override
    public void publishResourceForkEvent(ResourceForkMessage message) {
        String dedupKey = message.getForkTaskId();
        log.info("resourceFork published topic={} forkTaskId={} sourceResourceId={} resourceType={} version={}",
                MqTopicConstants.TOPIC_RESOURCE_FORK,
                message.getForkTaskId(),
                message.getSourceResourceId(),
                message.getResourceType(),
                message.getVersion());
        reliablePublisher.publish(MqTopicConstants.TOPIC_RESOURCE_FORK, dedupKey, message, dedupKey);
    }
}