package com.oriole.wisepen.resource.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisCacheManager {

    /** 阅读量去重窗口时长（分钟）*/
    @Value("${wisepen.resource.read-dedup-ttl-minutes:10}")
    private long readDedupTtlMinutes;

    private final StringRedisTemplate stringRedisTemplate;

    private static final String REDIS_READ_DEDUP_PREFIX = "wisepen:resource:read:dedup:";

    /**
     * 阅读去重：在 TTL 窗口内首次访问时置位，返回 true 表示窗口内首次阅读。
     */
    public Boolean isFirstReadInWindow(String resourceId, String userId) {
        String key = REDIS_READ_DEDUP_PREFIX + resourceId + ":" + userId;
        return stringRedisTemplate.opsForValue().setIfAbsent(key, "1", readDedupTtlMinutes, TimeUnit.MINUTES);
    }
}
