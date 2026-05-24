package com.oriole.wisepen.resource.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oriole.wisepen.resource.constant.SearchConstants;
import com.oriole.wisepen.resource.domain.entity.ESIndexEntity;
import com.oriole.wisepen.resource.domain.mq.AclRecalculateMessage;
import com.oriole.wisepen.resource.service.ISearchSyncService;
import com.oriole.wisepen.resource.util.ESIndexBuilder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.oriole.wisepen.resource.constant.MqTopicConstants.TOPIC_ACL_RECALC;

/**
 * 消费资源域 ACL 重算事件，把最新的 ACL 投影刷到 ES。
 * <p>
 * 与资源域的 {@code AclRecalculateConsumer} 共 topic、独立 groupId，互不抢分区。
 * <p>
 * 关键设计：<strong>延迟 3 秒</strong>后再通过 {@link ESIndexBuilder} 反查 Mongo 最新的
 * {@code ResourceItemEntity}（核心是其中已落库的 {@code computedGroupAcls}），
 * 再 {@code upsertIndexMetaData(...)}（仅元数据 + ACL + tags）。
 * <p>
 * 延迟的目的是等资源域 ACL 重算事务落库，避免读到旧 {@code computedGroupAcls}。
 */
@Slf4j
@Component
public class ESAclRecalcConsumer {

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private ESIndexBuilder esIndexBuilder;
    @Resource
    private ISearchSyncService searchSyncService;
    @Resource(name = "searchScheduledExecutorService")
    private ScheduledExecutorService searchScheduledExecutorService;

    /**
     * 兼容两种 payload 形态：
     * 上游若用 {@code spring-kafka} 默认 JsonDeserializer，会自动反序列化成 {@link AclRecalculateMessage}；
     * 若用 String，则手工 readValue。这里统一收 {@link Object}，按类型分支处理。
     */
    @KafkaListener(topics = TOPIC_ACL_RECALC, groupId = SearchConstants.CONSUMER_GROUP_ACL_RECALC)
    public void onAclRecalculate(Object payload) {
        AclRecalculateMessage msg = coerce(payload);
        if (msg == null || msg.getResourceId() == null) {
            log.warn("search aclRecalc skipped: invalid payload type={}",
                    payload == null ? "null" : payload.getClass().getName());
            return;
        }
        log.info("search aclRecalc received resourceId={} trigger={}",
                msg.getResourceId(), msg.getTriggerSource());

        // 延迟 3 秒后再反查 Mongo 拿到资源域刚落库的 computedGroupAcls
        searchScheduledExecutorService.schedule(() -> upsertSafely(msg),
                SearchConstants.ES_WRITE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void upsertSafely(AclRecalculateMessage msg) {
        try {
            Optional<ESIndexEntity> built = esIndexBuilder.build(msg.getResourceId(), null);
            built.ifPresent(searchSyncService::upsertIndexMetaData);
        } catch (Exception e) {
            log.error("search aclRecalc upsert failed resourceId={} trigger={}",
                    msg.getResourceId(), msg.getTriggerSource(), e);
        }
    }

    private AclRecalculateMessage coerce(Object payload) {
        if (payload instanceof AclRecalculateMessage m) {
            return m;
        }
        try {
            if (payload instanceof String s) {
                return objectMapper.readValue(s, AclRecalculateMessage.class);
            }
            // 兜底：万一是 LinkedHashMap 这类反序列化中间态，转一遍
            return objectMapper.convertValue(payload, AclRecalculateMessage.class);
        } catch (Exception e) {
            log.warn("search aclRecalc coerce failed payloadType={}",
                    payload == null ? "null" : payload.getClass().getName(), e);
            return null;
        }
    }
}
