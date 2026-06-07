package com.oriole.wisepen.note.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriole.wisepen.note.api.domain.mq.NoteOperationLogMessage;
import com.oriole.wisepen.note.service.INoteOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.oriole.wisepen.note.api.constant.MqTopicConstants.TOPIC_NOTE_OPLOG;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteOperationLogConsumer {

    private final INoteOperationLogService noteOperationLogService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TOPIC_NOTE_OPLOG,
            groupId = "wisepen-note-oplog-group",
            properties = {
                    "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"
            }
    )
    public void onOperationLog(String payload) throws Exception {
        // 从非Java微服务（NodeJS）的发布者订阅，使用objectMapper显式转换
        NoteOperationLogMessage msg = objectMapper.readValue(payload, NoteOperationLogMessage.class);
        log.info("note operation log event received. topic={} resourceId={} count={}",
                TOPIC_NOTE_OPLOG, msg.getResourceId(), msg.getEntries().size());
        try {
            noteOperationLogService.batchSave(msg);
            log.debug("note operation log event consumed. topic={} resourceId={} count={}",
                    TOPIC_NOTE_OPLOG, msg.getResourceId(), msg.getEntries().size());
        } catch (Exception e) {
            log.error("note operation log event consumption failed. topic={} resourceId={} count={}",
                    TOPIC_NOTE_OPLOG, msg.getResourceId(), msg.getEntries().size(), e);
            throw e;
        }
    }
}
