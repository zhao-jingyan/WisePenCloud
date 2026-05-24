package com.oriole.wisepen.resource.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriole.wisepen.resource.constant.SearchConstants;
import com.oriole.wisepen.resource.domain.entity.ESIndexEntity;
import com.oriole.wisepen.resource.domain.mq.ResourceDeletedMessage;
import com.oriole.wisepen.resource.enums.ResourceType;
import com.oriole.wisepen.resource.service.ISearchSyncService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.oriole.wisepen.resource.constant.MqTopicConstants.TOPIC_RESOURCE_PHYSICAL_DESTROY;

/**
 * 消费资源硬删除广播事件，把 ES 文档同步物理删除。
 * <p>
 * 按 {@link ResourceType} + {@code resourceId} 计算 {@code esId}（{@code <ext>_<resourceId>}），
 * 调用 {@link ISearchSyncService#deleteIndex(String)}，幂等删除。
 */
@Slf4j
@Component
public class ESPhysicalDestroyConsumer {

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ISearchSyncService searchSyncService;

    @KafkaListener(topics = TOPIC_RESOURCE_PHYSICAL_DESTROY,
            groupId = SearchConstants.CONSUMER_GROUP_PHYS_DESTROY,
            properties = {"spring.json.value.default.type=com.oriole.wisepen.resource.domain.mq.ResourceDeletedMessage"})
    public void onResourceDestroyed(Object payload) {
        ResourceDeletedMessage msg = coerce(payload);
        if (msg == null || msg.getTypedResourceIds() == null || msg.getTypedResourceIds().isEmpty()) {
            log.warn("search physicalDestroy skipped: invalid payload type={}",
                    payload == null ? "null" : payload.getClass().getName());
            return;
        }

        int total = 0;
        for (Map.Entry<ResourceType, List<String>> entry : msg.getTypedResourceIds().entrySet()) {
            ResourceType resourceType = entry.getKey();
            List<String> resourceIds = entry.getValue();
            if (resourceType == null || resourceIds == null || resourceIds.isEmpty()) {
                continue;
            }
            for (String resourceId : resourceIds) {
                String esId = ESIndexEntity.generateEsId(resourceType, resourceId);
                searchSyncService.deleteIndex(esId);
                total++;
            }
        }
        log.info("search physicalDestroy processed totalDeleted={}", total);
    }

    private ResourceDeletedMessage coerce(Object payload) {
        if (payload instanceof ResourceDeletedMessage m) {
            return m;
        }
        try {
            if (payload instanceof String s) {
                return objectMapper.readValue(s, ResourceDeletedMessage.class);
            }
            return objectMapper.convertValue(payload, ResourceDeletedMessage.class);
        } catch (Exception e) {
            log.warn("search physicalDestroy coerce failed payloadType={}",
                    payload == null ? "null" : payload.getClass().getName(), e);
            return null;
        }
    }
}
