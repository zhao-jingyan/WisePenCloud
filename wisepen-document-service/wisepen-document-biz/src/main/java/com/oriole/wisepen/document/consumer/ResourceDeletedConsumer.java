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
        if (!documentIds.isEmpty()) {
            log.info("接收到 Document 资源硬删除事件 resourceIds={}", documentIds);
            documentService.deleteDocuments(documentIds);
            log.info("已处理 Document 资源硬删除事件 resourceIds={}", documentIds);
        }
    }
}