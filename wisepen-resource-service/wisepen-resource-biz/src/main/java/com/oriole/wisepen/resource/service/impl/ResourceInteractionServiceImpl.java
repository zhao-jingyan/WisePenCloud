package com.oriole.wisepen.resource.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.resource.cache.RedisCacheManager;
import com.oriole.wisepen.resource.domain.dto.req.ResourceRateRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceLikeRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceReadRequest;
import com.oriole.wisepen.resource.domain.dto.res.ResourceUserInteractionRecordResponse;
import com.oriole.wisepen.resource.domain.entity.ResourceUserInteractionRecordEntity;
import com.oriole.wisepen.resource.exception.ResourceError;
import com.oriole.wisepen.resource.repository.CustomResourceInteractionInfoRepository;
import com.oriole.wisepen.resource.repository.CustomResourceUserInteractionRecordRepository;
import com.oriole.wisepen.resource.repository.ResourceItemRepository;
import com.oriole.wisepen.resource.repository.ResourceUserInteractRecordRepository;
import com.oriole.wisepen.resource.service.IResourceInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceInteractionServiceImpl implements IResourceInteractionService {

    private final ResourceItemRepository resourceItemRepository;
    private final ResourceUserInteractRecordRepository resourceUserInteractRecordRepository;
    private final CustomResourceInteractionInfoRepository customResourceInteractionInfoRepository;
    private final CustomResourceUserInteractionRecordRepository customResourceUserInteractionRecordRepository;

    private final RedisCacheManager redisCacheManager;

    @Override
    public ResourceUserInteractionRecordResponse getResourceUserInteractionInfo(String resourceId, String userId){
        ResourceUserInteractionRecordEntity interactionRecord = resourceUserInteractRecordRepository
                .findByUserIdAndResourceId(userId, resourceId).orElse(
                        new ResourceUserInteractionRecordEntity(resourceId, userId)
                );
        return BeanUtil.copyProperties(interactionRecord, ResourceUserInteractionRecordResponse.class);
    }

    /**
     * 阅读
     */
    @Override
    public void changeResourceReadStatus(ResourceReadRequest request, String userId){
        if (!resourceItemRepository.existsById(request.getResourceId())) {
            throw new ServiceException(ResourceError.RESOURCE_NOT_FOUND);
        }

        String resourceId = request.getResourceId();

        Boolean isFirstReadInWindow = redisCacheManager.isFirstReadInWindow(resourceId, userId);
        if (Boolean.TRUE.equals(isFirstReadInWindow)) {
            customResourceUserInteractionRecordRepository.findAndSetRead(resourceId, userId, true);
            customResourceInteractionInfoRepository.incrementReadCount(resourceId, 1);
            log.info("readCount incremented resourceId={} userId={}", resourceId, userId);
        }
    }

    /**
     * 点赞/取消点赞
     */
    @Override
    public void changeResourceLikeStatus(ResourceLikeRequest request, String userId) {
        if (!resourceItemRepository.existsById(request.getResourceId())) {
            throw new ServiceException(ResourceError.RESOURCE_NOT_FOUND);
        }

        String resourceId = request.getResourceId();

        boolean currentLiked = resourceUserInteractRecordRepository
                .findByUserIdAndResourceId(userId, resourceId)
                .map(r -> Boolean.TRUE.equals(r.getLiked()))
                .orElse(false);
        boolean wantLiked = !currentLiked;

        customResourceUserInteractionRecordRepository.findAndSetLiked(resourceId, userId, wantLiked);
        customResourceInteractionInfoRepository.incrementLikeCount(resourceId, wantLiked ? 1 : -1);

        log.info("resource like toggled resourceId={} userId={} wantLiked={}", resourceId, userId, wantLiked);
    }

    /**
     * 资源评分
     */
    @Override
    public void changeResourceScore(ResourceRateRequest request, String userId) {
        if (!resourceItemRepository.existsById(request.getResourceId())) {
            throw new ServiceException(ResourceError.RESOURCE_NOT_FOUND);
        }

        Integer newScore = request.getScore();
        String resourceId = request.getResourceId();

        ResourceUserInteractionRecordEntity oldRecord =
                customResourceUserInteractionRecordRepository.findAndSetScore(resourceId, userId, newScore);

        Integer oldScore = oldRecord == null ? null : oldRecord.getScore();
        if (oldScore == null) { // 首次评分
            customResourceInteractionInfoRepository.updateScoreStats(resourceId, 1, newScore);
        } else if (!oldScore.equals(newScore)) { // 改分
            customResourceInteractionInfoRepository.updateScoreStats(resourceId, 0, newScore - oldScore);
        }
        log.info("resource rated resourceId={} userId={} oldScore={} newScore={}", resourceId, userId, oldScore, newScore);
    }
}
