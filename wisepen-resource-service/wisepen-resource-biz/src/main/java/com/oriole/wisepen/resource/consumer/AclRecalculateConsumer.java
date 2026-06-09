package com.oriole.wisepen.resource.consumer;

import com.oriole.wisepen.resource.domain.mq.AclRecalculateMessage;
import com.oriole.wisepen.resource.enums.UpsertField;
import com.oriole.wisepen.resource.service.IResourceService;
import com.oriole.wisepen.resource.service.ISearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

import static com.oriole.wisepen.resource.constant.MqTopicConstants.TOPIC_ACL_RECALC;

@Slf4j
@Component
@RequiredArgsConstructor
public class AclRecalculateConsumer {

    private final IResourceService resourceService;
    private final ISearchSyncService searchSyncService;

    @KafkaListener(topics = TOPIC_ACL_RECALC, groupId = "wisepen-resource-acl-recalc-group")
    public void onAclRecalculate(AclRecalculateMessage message) {
        log.info("acl recalculation event received. topic={} resourceId={} trigger={}",
                TOPIC_ACL_RECALC, message.getResourceId(), message.getTriggerSource());
        try {
            resourceService.calculateResourceGroupAcl(message.getResourceId())
                    .ifPresent(resourceItemEntity ->
                            searchSyncService.syncResourceMetadata(resourceItemEntity, EnumSet.of(UpsertField.ACL))
                    );
            log.debug("acl recalculation event consumed. topic={} resourceId={}", TOPIC_ACL_RECALC, message.getResourceId());
        } catch (Exception e) {
            log.error("acl recalculation event consumption failed. topic={} resourceId={}", TOPIC_ACL_RECALC, message.getResourceId(), e);
            throw e;
        }
    }
}
