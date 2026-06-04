package com.oriole.wisepen.resource.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriole.wisepen.document.api.domain.mq.DocumentReadyMessage;
import com.oriole.wisepen.note.api.domain.mq.NoteSnapshotMessage;
import com.oriole.wisepen.resource.service.ISearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.oriole.wisepen.document.api.constant.MqTopicConstants.TOPIC_DOCUMENT_READY;
import static com.oriole.wisepen.note.api.constant.MqTopicConstants.TOPIC_NOTE_SNAPSHOT;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceContentConsumer {

    private final ISearchSyncService searchSyncService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = TOPIC_DOCUMENT_READY, groupId = "wisepen-document-ready-group")
    public void onDocumentReady(DocumentReadyMessage message) {
        log.info("documentReady received topic={} resourceId={} contentLength={}",
                TOPIC_DOCUMENT_READY, message.getResourceId(), message.getContent()!=null ? message.getContent().length() : 0);
        try {
            searchSyncService.syncResourceContent(message.getResourceId(), message.getContent());
        } catch (Exception e) {
            log.error("documentReady consume failed topic={} resourceId={}", TOPIC_DOCUMENT_READY, message.getResourceId(), e);
        }
    }

    @KafkaListener(
            topics = TOPIC_NOTE_SNAPSHOT,
            groupId = "wisepen-note-snapshot-group",
            properties = {
                    "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"
            }
    )
    public void onSnapshot(String payload) throws JsonProcessingException {
        // 从非Java微服务（NodeJS）的发布者订阅，使用objectMapper显式转换
        NoteSnapshotMessage msg = objectMapper.readValue(payload, NoteSnapshotMessage.class);
        log.info("noteSnapshot received topic={} resourceId={} contentLength={}",
                TOPIC_NOTE_SNAPSHOT, msg.getResourceId(), msg.getPlainText()!=null ? msg.getPlainText().length() : 0);
        try {
            if ("FULL".equals(msg.getType())) searchSyncService.syncResourceContent(msg.getResourceId(), msg.getPlainText());

        } catch (Exception e) {
            log.error("documentReady consume failed topic={} resourceId={}", TOPIC_NOTE_SNAPSHOT, msg.getResourceId(), e);
        }
    }
}
