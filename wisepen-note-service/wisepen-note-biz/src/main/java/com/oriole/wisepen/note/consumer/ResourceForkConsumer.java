package com.oriole.wisepen.note.consumer;

import com.oriole.wisepen.note.service.INoteService;
import com.oriole.wisepen.resource.domain.mq.ResourceForkMessage;
import com.oriole.wisepen.resource.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.oriole.wisepen.resource.constant.MqTopicConstants.TOPIC_RESOURCE_FORK;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceForkConsumer {

    private final INoteService noteService;

    @KafkaListener(topics = TOPIC_RESOURCE_FORK, groupId = "wisepen-note-fork-group")
    public void onResourceFork(ResourceForkMessage msg) {
        if (msg.getResourceType() != ResourceType.NOTE) {
            return;
        }
        log.info("resourceFork received topic={} forkTaskId={} sourceResourceId={} resourceType={}",
                TOPIC_RESOURCE_FORK, msg.getForkTaskId(), msg.getSourceResourceId(), msg.getResourceType());
        try {
            noteService.forkNote(msg);
            log.debug("resourceFork consumed topic={} forkTaskId={} sourceResourceId={} resourceType={}",
                    TOPIC_RESOURCE_FORK, msg.getForkTaskId(), msg.getSourceResourceId(), msg.getResourceType());
        } catch (Exception e) {
            log.error("resourceFork consume failed topic={} forkTaskId={} sourceResourceId={} resourceType={}",
                    TOPIC_RESOURCE_FORK, msg.getForkTaskId(), msg.getSourceResourceId(), msg.getResourceType(), e);
            throw e;
        }
    }
}
