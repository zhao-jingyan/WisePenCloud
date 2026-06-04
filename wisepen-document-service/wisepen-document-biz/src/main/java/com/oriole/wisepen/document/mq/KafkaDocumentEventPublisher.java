package com.oriole.wisepen.document.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriole.wisepen.common.mq.ReliablePublisher;
import com.oriole.wisepen.document.api.domain.mq.DocumentParseTaskMessage;
import com.oriole.wisepen.document.api.domain.mq.DocumentReadyMessage;
import com.oriole.wisepen.file.storage.api.domain.mq.FileUploadedMessage;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.oriole.wisepen.document.api.constant.MqTopicConstants.TOPIC_DOCUMENT_PARSE;
import static com.oriole.wisepen.document.api.constant.MqTopicConstants.TOPIC_DOCUMENT_READY;
import static com.oriole.wisepen.file.storage.api.constant.MqTopicConstants.TOPIC_FILE_DELETE;

/**
 * 文档服务 Kafka 事件发布器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDocumentEventPublisher {

    @Resource
    ReliablePublisher reliablePublisher;
    private final ObjectMapper objectMapper;

    // 发布文档解析任务（内部削峰）
    public void publishParseTask(DocumentParseTaskMessage msg) {
        try {
            reliablePublisher.publish(TOPIC_DOCUMENT_PARSE, msg.getDocumentId(), msg, msg.getDocumentId());
            log.debug("成功发布文档解析事件 document: {}", msg.getDocumentId());
        } catch (Exception e) {
            log.error("发布文档解析事件失败 document: {}", msg.getDocumentId(), e);
        }
    }

    // 发布文档处理就绪事件
    public void publishReadyEvent(DocumentReadyMessage msg) {
        try {
            reliablePublisher.publish(TOPIC_DOCUMENT_READY, msg.getResourceId(), msg, msg.getResourceId());
            log.debug("成功发布文档就绪事件 document: {}", msg.getResourceId());
        } catch (Exception e) {
            log.error("发布文档就绪事件失败 document: {}", msg.getResourceId(), e);
        }
    }

    // 发布文件删除事件
    public void publishFileDeleteEvent(List<String> allObjectKeys) {
        try {
            // 发布至兼容非Java微服务的订阅者，统一使用 Jackson 序列化
            String jsonPayload = objectMapper.writeValueAsString(allObjectKeys);
            reliablePublisher.publish(TOPIC_FILE_DELETE, null, jsonPayload, null);
            log.debug("成功发布文档删除事件 Document: {}", allObjectKeys);
        } catch (Exception e) {
            log.error("发布文档删除事件失败 Document: {}", allObjectKeys, e);
        }
    }
}