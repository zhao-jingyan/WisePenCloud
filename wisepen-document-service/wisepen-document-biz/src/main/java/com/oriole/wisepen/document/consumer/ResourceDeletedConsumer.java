package com.oriole.wisepen.document.consumer;

import com.oriole.wisepen.document.api.constant.DocumentConstants;
import com.oriole.wisepen.document.service.IDocumentService;
import com.oriole.wisepen.resource.domain.mq.ResourceDeletedMessage;
import com.oriole.wisepen.resource.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.oriole.wisepen.common.core.util.LogIdUtils.summarizeIds;
import static com.oriole.wisepen.resource.constant.MqTopicConstants.TOPIC_RESOURCE_PHYSICAL_DESTROY;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceDeletedConsumer {

    private final IDocumentService documentService;

    @KafkaListener(topics = TOPIC_RESOURCE_PHYSICAL_DESTROY, groupId = "wisepen-resource-physical-destroy-group")
    public void onResourceDeleted(ResourceDeletedMessage message) {
        Map<ResourceType, List<String>> typedMap = message.getTypedResourceIds();
        List<String> documentIds = new ArrayList<>();
        for (ResourceType allowedType : DocumentConstants.ALLOWED_TYPES) {
            List<String> idsForType = typedMap.get(allowedType);
            if (idsForType != null && !idsForType.isEmpty()) {
                documentIds.addAll(idsForType);
            }
        }
        log.info("document resource delete event received. topic={} count={} documentIds={}",
                TOPIC_RESOURCE_PHYSICAL_DESTROY, documentIds.size(), summarizeIds(documentIds));
        if (!documentIds.isEmpty()) {
            try {
                documentService.deleteDocuments(documentIds);
                log.debug("document resource delete event consumed. topic={} count={} documentIds={}",
                        TOPIC_RESOURCE_PHYSICAL_DESTROY, documentIds.size(), summarizeIds(documentIds));
            } catch (Exception e) {
                log.error("document resource delete event consumption failed. topic={} count={} documentIds={}",
                        TOPIC_RESOURCE_PHYSICAL_DESTROY, documentIds.size(), summarizeIds(documentIds), e);
                throw e;
            }
        } else {
            log.debug("document resource delete event skipped because no document resources. topic={}",
                    TOPIC_RESOURCE_PHYSICAL_DESTROY);
        }
    }
}
