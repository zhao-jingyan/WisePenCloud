package com.oriole.wisepen.resource.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriole.wisepen.resource.constant.SearchConstants;
import com.oriole.wisepen.resource.domain.entity.ESIndexEntity;
import com.oriole.wisepen.resource.service.ISearchSyncService;
import com.oriole.wisepen.resource.util.ESIndexBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 消费上游 Document 服务发出的 {@code wisepen-document-ready-topic}：文档解析完成、可写入正文索引。
 * <p>
 * 行为：{@link ESIndexBuilder#build(String, String)} → {@code upsertFullIndex(...)}（首次入库走全量 Upsert）。
 * <p>
 * 与 Document 服务跨模块复用消息类不可行（避免互依赖），所以直接收 String，按一份本地 POJO 反序列化。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ESUpsertDocumentConsumer {

    /** 与 wisepen-document-api 中定义保持一致 */
    public static final String TOPIC_DOCUMENT_READY = "wisepen-document-ready-topic";

    private final ObjectMapper objectMapper;
    private final ESIndexBuilder esIndexBuilder;
    private final ISearchSyncService searchSyncService;

    @KafkaListener(
            topics = TOPIC_DOCUMENT_READY,
            groupId = SearchConstants.CONSUMER_GROUP_DOC_READY,
            properties = {"value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"}
    )
    public void onDocumentReady(String payload) {
        DocumentReadyMessage msg;
        try {
            msg = objectMapper.readValue(payload, DocumentReadyMessage.class);
        } catch (Exception e) {
            log.error("search docReady parse failed payload={}", payload, e);
            return;
        }
        log.info("search docReady received topic={} resourceId={} contentLength={}",
                TOPIC_DOCUMENT_READY, msg.getResourceId(),
                msg.getContent() == null ? 0 : msg.getContent().length());
        try {
            Optional<ESIndexEntity> built = esIndexBuilder.build(msg.getResourceId(), msg.getContent());
            built.ifPresent(searchSyncService::upsertFullIndex);
        } catch (Exception e) {
            log.error("search docReady upsert failed resourceId={}", msg.getResourceId(), e);
            // 不向上抛：避免一条坏消息卡死消费分区；由 Document 端重试或运维人工触发重建
        }
    }

    /** 与 {@code com.oriole.wisepen.document.api.domain.mq.DocumentReadyMessage} 保持字段兼容 */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DocumentReadyMessage {
        private String resourceId;
        private String content;
    }
}
