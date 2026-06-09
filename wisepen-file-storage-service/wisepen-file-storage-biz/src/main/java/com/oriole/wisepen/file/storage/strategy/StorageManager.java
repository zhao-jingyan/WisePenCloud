package com.oriole.wisepen.file.storage.strategy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.file.storage.domain.entity.StorageConfigEntity;
import com.oriole.wisepen.file.storage.exception.FileStorageError;
import com.oriole.wisepen.file.storage.mapper.StorageConfigMapper;
import com.oriole.wisepen.file.storage.strategy.aliyun.AliyunOssProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储多实例路由器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageManager {

    private final StorageConfigMapper storageConfigMapper;

    // 存储实例缓存池
    private final Map<Long, StorageProvider> providerCache = new ConcurrentHashMap<>();

    // 首要存储源 ID
    private Long primaryConfigId;

    /**
     * 系统启动时自动加载并初始化所有启用的存储实例
     */
    @PostConstruct
    public synchronized void init() {
        log.info("storage provider load started. task=storageManagerInit");
        // 销毁旧实例
        destroyAll();

        List<StorageConfigEntity> configs = storageConfigMapper.selectList(
                Wrappers.<StorageConfigEntity>lambdaQuery().eq(StorageConfigEntity::getEnabled, true)
        );

        for (StorageConfigEntity config : configs) {
            try {
                StorageProvider provider = buildProvider(config);
                providerCache.put(config.getId(), provider);

                if (Boolean.TRUE.equals(config.getIsPrimary())) {
                    this.primaryConfigId = config.getId();
                }
            } catch (Exception e) {
                log.error("storage provider init failed. configId={} provider={}",
                        config.getId(), config.getProvider(), e);
            }
        }

        if (this.primaryConfigId == null && !providerCache.isEmpty()) {
            this.primaryConfigId = providerCache.keySet().iterator().next();
            log.warn("primary storage provider degraded. primaryConfigId={}",
                    this.primaryConfigId);
        }
        log.info("storage provider load finished. task=storageManagerInit providerCount={} primaryConfigId={}",
                providerCache.size(), primaryConfigId);
    }

    /**
     * 根据具体提供商类型，通过工厂方法构建实例
     */
    private StorageProvider buildProvider(StorageConfigEntity config) {
        return switch (config.getProvider()) {
            case ALIYUN_OSS -> new AliyunOssProvider(config);
            // 预留其他云厂商的扩展口
            // case MINIO -> new MinioProvider(config);
            default -> throw new ServiceException(FileStorageError.CANNOT_SUPPORT_STORAGE_PROVIDER);
        };
    }

    /**
     * 获取首要存储实例
     */
    public StorageProvider getPrimaryProvider() {
        StorageProvider provider = providerCache.get(primaryConfigId);
        if (provider == null) {
            throw new ServiceException(FileStorageError.CANNOT_SUPPORT_STORAGE_PROVIDER);
        }
        return provider;
    }

    /**
     * 根据 ConfigId 获取指定存储实例
     */
    public StorageProvider getProvider(Long configId) {
        if (configId == null) {
            return getPrimaryProvider();
        }
        StorageProvider provider = providerCache.get(configId);
        if (provider == null) {
            throw new ServiceException(FileStorageError.CANNOT_SUPPORT_STORAGE_PROVIDER);
        }
        return provider;
    }

    /**
     * 根据 Domain 反向路由获取存储实例 (用于根据 URL 删除文件)
     */
    public StorageProvider getProviderByDomain(String domain) {
        for (StorageProvider provider : providerCache.values()) {
            // 简单去斜杠匹配
            if (provider.getDomain().replaceAll("/$", "").equals(domain.replaceAll("/$", ""))) {
                return provider;
            }
        }
        throw new ServiceException(FileStorageError.FILE_URL_INVALID);
    }

    /**
     * 获取所有启用的存储实例 (用于兜底遍历查询)
     */
    public List<StorageProvider> getAllProviders() {
        return new ArrayList<>(providerCache.values());
    }

    @PreDestroy
    public void destroyAll() {
        int providerCount = providerCache.size();
        providerCache.values().forEach(StorageProvider::destroy);
        providerCache.clear();
        log.info("storage provider destroyed. providerCount={}", providerCount);
    }
}
