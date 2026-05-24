package com.oriole.wisepen.resource.config;

import com.oriole.wisepen.resource.constant.SearchConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 搜索域专用调度线程池。
 * <p>
 * 仅供 {@code ESAclRecalcConsumer} 与 {@code ESUpsertNoteSnapshotConsumer}
 * 的 3 秒延迟反查使用，与 Spring 默认 TaskScheduler 隔离，
 * 防止抢占资源域主流程线程。
 */
@Configuration
public class SearchThreadPoolConfig {

    @Bean(name = "searchScheduledExecutorService", destroyMethod = "shutdown")
    public ScheduledExecutorService searchScheduledExecutorService() {
        return Executors.newScheduledThreadPool(SearchConstants.COREPOOLSIZE, new NamedThreadFactory());
    }

    /** 命名线程，便于在 dump / 日志里识别 */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger seq = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "search-scheduler-" + seq.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
}
