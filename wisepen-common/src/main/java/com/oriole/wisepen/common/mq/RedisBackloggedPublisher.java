package com.oriole.wisepen.common.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.oriole.wisepen.common.mq.MqConstants.BACKLOG_KEY_PREFIX;
import static com.oriole.wisepen.common.mq.MqConstants.BACKLOG_LOCK_PREFIX;
import static com.oriole.wisepen.common.mq.MqConstants.BACKLOG_TTL;
import static com.oriole.wisepen.common.mq.MqConstants.DRAIN_LOCK_TTL;

@Slf4j
@RequiredArgsConstructor
public class RedisBackloggedPublisher implements ReliablePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public CompletableFuture<Void> publish(String topic, String kafkaKey, Object payload, String dedupKey) {

        if (dedupKey == null) dedupKey = kafkaKey != null ? kafkaKey : UUID.randomUUID().toString();

        drainBacklogAsync(topic); // 每次发新消息前，先异步尝试补发这个 topic 之前失败积压的消息

        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            // 发送当前消息
            String finalDedupKey = dedupKey;
            kafkaTemplate.send(topic, finalDedupKey, payload).whenComplete((sendResult, ex) -> {
                if (ex == null) {
                    log.debug("message published. topic={} key={} dedupKey={}", topic, kafkaKey, finalDedupKey);
                    result.complete(null);
                    return;
                }
                // 如果 Kafka 异步回调里失败
                enqueueBacklog(topic, kafkaKey, payload, finalDedupKey, ex);
                result.completeExceptionally(ex);
            });
        } catch (Exception e) { // 如果 kafkaTemplate.send(...) 本身直接抛异常
            enqueueBacklog(topic, kafkaKey, payload, dedupKey, e);
            result.completeExceptionally(e);
        }
        return result;
    }

    private void drainBacklogAsync(String topic) {
        CompletableFuture.runAsync(() -> {
            try {
                drainBacklog(topic);
            } catch (Exception e) {
                log.warn("backlog drain failed. topic={}", topic, e);
            }
        });
    }

    void drainBacklog(String topic) {
        String lockKey = BACKLOG_LOCK_PREFIX + topic;
        // 先加锁
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", DRAIN_LOCK_TTL);
        if (!Boolean.TRUE.equals(locked)) {
            // 拿不到锁就直接返回，避免多个实例重复 drain
            return;
        }

        String backlogKey = BACKLOG_KEY_PREFIX + topic;
        int succeeded = 0;
        int failed = 0;
        try {
            Map<Object, Object> snapshot = redisTemplate.opsForHash().entries(backlogKey);
            if (snapshot.isEmpty()) {
                return;
            }

            for (Map.Entry<Object, Object> entry : snapshot.entrySet()) {
                // 取出 dedupKey
                String dedupKey = Objects.toString(entry.getKey(), null);
                // 把 Redis hash value 转成 BacklogMessage
                BacklogMessage backlogMessage = objectMapper.convertValue(entry.getValue(), BacklogMessage.class);
                try {
                    // 根据 payloadType + payloadJson 反序列化 payload
                    Object payload = deserializePayload(backlogMessage);
                    // 发送 Kafka
                    kafkaTemplate.send(backlogMessage.getTopic(), backlogMessage.getKafkaKey(), payload).get();
                    // 发送成功后，从 Redis hash 删除这条 backlog
                    redisTemplate.opsForHash().delete(backlogKey, dedupKey);
                    succeeded++;
                } catch (Exception e) {
                    // 失败则保留在 Redis，下次再补偿
                    failed++;
                    log.warn("backlog drain failed. topic={} dedupKey={}", topic, dedupKey, e);
                }
            }

            log.info("backlog drained. topic={} succeeded={} failed={}", topic, succeeded, failed);
        } finally {
            // 释放锁
            redisTemplate.delete(lockKey);
        }
    }

    private void enqueueBacklog(String topic, String kafkaKey, Object payload, String dedupKey, Throwable publishError) {
        String backlogKey = BACKLOG_KEY_PREFIX + topic;
        try {
            // 包装失败消息
            BacklogMessage backlogMessage = new BacklogMessage(
                    topic,
                    kafkaKey,
                    payload.getClass().getName(),
                    objectMapper.writeValueAsString(payload)
            );
            // 写入 Redis，相同 dedupKey 的失败消息会被覆盖
            redisTemplate.opsForHash().put(backlogKey, dedupKey, backlogMessage);
            redisTemplate.expire(backlogKey, BACKLOG_TTL);
            log.warn("message publish failed. topic={} key={} dedupKey={} enqueued=true",
                    topic, kafkaKey, dedupKey, publishError);
        } catch (Exception enqueueError) {
            log.error("backlog enqueue failed. topic={} key={} dedupKey={} payload={}",
                    topic, kafkaKey, dedupKey, payload, enqueueError);
        }
    }

    private Object deserializePayload(BacklogMessage backlogMessage) throws ClassNotFoundException, JsonProcessingException {
        Class<?> payloadType = Class.forName(backlogMessage.getPayloadType());
        return objectMapper.readValue(backlogMessage.getPayloadJson(), payloadType);
    }
}
