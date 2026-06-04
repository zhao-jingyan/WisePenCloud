package com.oriole.wisepen.ai.asset.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriole.wisepen.common.mq.ReliablePublisher;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.oriole.wisepen.file.storage.api.constant.MqTopicConstants.TOPIC_FILE_DELETE;

/**
 * 文档服务 Kafka 事件发布器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaSkillEventPublisher {

    @Resource
    ReliablePublisher reliablePublisher;
    private final ObjectMapper objectMapper;

    // 发布文件删除事件
    public void publishFileDeleteEvent(List<String> allObjectKeys) {
        try {
            // 发布至兼容非Java微服务的订阅者，统一使用 Jackson 序列化
            String jsonPayload = objectMapper.writeValueAsString(allObjectKeys);
            reliablePublisher.publish(TOPIC_FILE_DELETE, null, jsonPayload, null);
            log.debug("成功发布文档删除事件 Document: {}", allObjectKeys);
        } catch (Exception e) {
            log.error("发布文档解析删除失败 Document: {}", allObjectKeys, e);
        }
    }
}