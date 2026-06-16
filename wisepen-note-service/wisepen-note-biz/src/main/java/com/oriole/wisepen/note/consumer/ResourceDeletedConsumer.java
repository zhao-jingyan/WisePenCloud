package com.oriole.wisepen.note.consumer;

import com.oriole.wisepen.note.service.impl.NoteServiceImpl;
import com.oriole.wisepen.resource.domain.mq.ResourceDeletedMessage;
import com.oriole.wisepen.resource.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.oriole.wisepen.common.core.util.LogIdUtils.summarizeIds;
import static com.oriole.wisepen.resource.constant.MqTopicConstants.TOPIC_RESOURCE_PHYSICAL_DESTROY;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceDeletedConsumer {

    private final NoteServiceImpl noteService;

    @KafkaListener(topics = TOPIC_RESOURCE_PHYSICAL_DESTROY, groupId = "wisepen-note-physical-destroy-group")
    public void onResourceDeleted(ResourceDeletedMessage message) {
        Map<ResourceType, List<String>> typedMap = message.getTypedResourceIds();
        List<String> noteIds = typedMap.get(ResourceType.NOTE);
        int count = noteIds == null ? 0 : noteIds.size();
        log.info("note delete event received. topic={} count={} noteIds={}",
                TOPIC_RESOURCE_PHYSICAL_DESTROY, count, summarizeIds(noteIds));
        if (count > 0) {
            try {
                noteService.deleteNotes(noteIds);
                log.debug("note delete event consumed. topic={} count={} noteIds={}",
                        TOPIC_RESOURCE_PHYSICAL_DESTROY, count, summarizeIds(noteIds));
            } catch (Exception e) {
                log.error("note delete event consumption failed. topic={} count={} noteIds={}",
                        TOPIC_RESOURCE_PHYSICAL_DESTROY, count, summarizeIds(noteIds), e);
                throw e;
            }
        } else {
            log.info("note delete event skipped because no note resources. topic={}",
                    TOPIC_RESOURCE_PHYSICAL_DESTROY);
        }
    }
}
