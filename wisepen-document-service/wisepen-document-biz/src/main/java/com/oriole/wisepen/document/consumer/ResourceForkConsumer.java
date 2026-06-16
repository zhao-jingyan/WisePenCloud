package com.oriole.wisepen.document.consumer;

import com.oriole.wisepen.document.service.IDocumentService;
import com.oriole.wisepen.document.api.constant.DocumentConstants;
import com.oriole.wisepen.resource.domain.mq.ResourceForkMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.oriole.wisepen.resource.constant.MqTopicConstants.TOPIC_RESOURCE_FORK;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceForkConsumer {

    private final IDocumentService documentService;

    @KafkaListener(topics = TOPIC_RESOURCE_FORK, groupId = "wisepen-document-fork-group")
    public void onResourceFork(ResourceForkMessage msg) {
        if (!DocumentConstants.ALLOWED_TYPES.contains(msg.getResourceType())) {
            return;
        }

        log.info("resourceFork received topic={} forkTaskId={} sourceResourceId={} resourceType={}",
                TOPIC_RESOURCE_FORK, msg.getForkTaskId(), msg.getSourceResourceId(), msg.getResourceType());
        try {
            documentService.forkDocument(msg);
            log.debug("resourceFork consumed topic={} forkTaskId={} sourceResourceId={} resourceType={}",
                    TOPIC_RESOURCE_FORK, msg.getForkTaskId(), msg.getSourceResourceId(), msg.getResourceType());
        } catch (Exception e) {
            log.error("resourceFork consume failed topic={} forkTaskId={} sourceResourceId={} resourceType={}",
                    TOPIC_RESOURCE_FORK, msg.getForkTaskId(), msg.getSourceResourceId(), msg.getResourceType(), e);
            throw e;
        }
    }

}
