package com.oriole.wisepen.file.storage.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriole.wisepen.common.mq.ReliablePublisher;
import com.oriole.wisepen.file.storage.api.domain.mq.FileUploadedMessage;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.oriole.wisepen.file.storage.api.constant.MqTopicConstants.TOPIC_FILE_UPLOADED;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaStorageEventPublisher {

    @Resource
    ReliablePublisher reliablePublisher;

    private final ObjectMapper objectMapper;

    public void publishFileUploadedEvent(FileUploadedMessage msg) {
        try {
            String objectKey = msg.getObjectKey();
            String md5 = msg.getMd5();
            // （可能）发布至非Java微服务的订阅者，统一使用 Jackson 序列化
            String jsonMessage = objectMapper.writeValueAsString(msg);
            // 将 MD5 作为 Kafka 的 Key，保证相同文件的消息能被路由到同一个 Partition，保证顺序消费
            reliablePublisher.publish(TOPIC_FILE_UPLOADED, md5, jsonMessage, md5);
            log.debug("file uploaded event publish requested. topic={} objectKey={} md5={}",
                    TOPIC_FILE_UPLOADED, objectKey, md5);
        } catch (Exception e) {
            log.error("file uploaded event publish request failed. topic={} objectKey={}",
                    TOPIC_FILE_UPLOADED, msg.getObjectKey(), e);
        }
    }
}
