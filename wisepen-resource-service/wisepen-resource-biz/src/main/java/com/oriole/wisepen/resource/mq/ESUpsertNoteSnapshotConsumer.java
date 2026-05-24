package com.oriole.wisepen.resource.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriole.wisepen.resource.constant.SearchConstants;
import com.oriole.wisepen.resource.domain.entity.ESIndexEntity;
import com.oriole.wisepen.resource.service.ISearchSyncService;
import com.oriole.wisepen.resource.util.ESIndexBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 消费上游 Note 服务发出的 {@code wisepen-note-snapshot-topic}：协同笔记发布快照，刷新正文索引。
 * <p>
 * 关键设计：
 * <ul>
 *   <li>仅处理 {@code type=FULL} 的检查点快照（带 plainText），忽略 DELTA。</li>
 *   <li><strong>延迟 3 秒</strong>后再 Upsert（仅写 content），避免与 Yjs 协同写入竞态。
 *       使用搜索域专用调度池 {@code searchScheduledExecutorService}，不抢占主链路线程。</li>
 *   <li>失败仅记日志，等下一次 FULL 快照重试。</li>
 * </ul>
 */
@Slf4j
@Component
public class ESUpsertNoteSnapshotConsumer {

    /** 与 wisepen-note-api 中定义保持一致 */
    public static final String TOPIC_NOTE_SNAPSHOT = "wisepen-note-snapshot-topic";

    private static final String SNAPSHOT_TYPE_FULL = "FULL";

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ESIndexBuilder esIndexBuilder;
    @Resource
    private ISearchSyncService searchSyncService;
    @Resource(name = "searchScheduledExecutorService")
    private ScheduledExecutorService searchScheduledExecutorService;

    @KafkaListener(
            topics = TOPIC_NOTE_SNAPSHOT,
            groupId = SearchConstants.CONSUMER_GROUP_NOTE_SNAPSHOT,
            properties = {"value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"}
    )
    public void onNoteSnapshot(String payload) {
        NoteSnapshotMessage msg;
        try {
            msg = objectMapper.readValue(payload, NoteSnapshotMessage.class);
        } catch (Exception e) {
            log.error("search noteSnapshot parse failed payload={}", payload, e);
            return;
        }

        // 只处理 FULL 检查点：plainText 仅在 FULL 类型快照里提供
        if (!SNAPSHOT_TYPE_FULL.equalsIgnoreCase(msg.getType())) {
            log.debug("search noteSnapshot skipped non-FULL resourceId={} type={}", msg.getResourceId(), msg.getType());
            return;
        }

        log.info("search noteSnapshot received resourceId={} version={} plainTextLength={}",
                msg.getResourceId(), msg.getVersion(),
                msg.getPlainText() == null ? 0 : msg.getPlainText().length());

        // 延迟 3 秒，避免和 Yjs 协同写入竞态把过时内容落进 ES
        searchScheduledExecutorService.schedule(() -> upsertSafely(msg),
                SearchConstants.ES_WRITE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void upsertSafely(NoteSnapshotMessage msg) {
        try {
            Optional<ESIndexEntity> built = esIndexBuilder.build(msg.getResourceId(), msg.getPlainText());
            built.ifPresent(searchSyncService::upsertIndexContent);
        } catch (Exception e) {
            log.error("search noteSnapshot upsert failed resourceId={} version={}",
                    msg.getResourceId(), msg.getVersion(), e);
            // 跳过本次同步，等待后续快照重试
        }
    }

    /** 与 {@code com.oriole.wisepen.note.api.domain.mq.NoteSnapshotMessage} 保持字段兼容（按需保留字段） */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NoteSnapshotMessage {
        private String resourceId;
        private Long version;
        /** FULL = 检查点全量；DELTA = 增量 */
        private String type;
        /** Base64 编码的 Yjs 二进制数据；搜索域不消费 */
        private String data;
        /** FULL 检查点时 Node 提取的纯文本，用于全文检索 */
        private String plainText;
        /** 本轮活跃编辑用户列表；搜索域不消费 */
        private java.util.List<String> updatedBy;
    }
}
