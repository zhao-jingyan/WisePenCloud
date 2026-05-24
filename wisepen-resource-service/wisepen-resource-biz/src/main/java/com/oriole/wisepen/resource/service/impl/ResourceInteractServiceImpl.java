package com.oriole.wisepen.resource.service.impl;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.resource.domain.dto.req.ResourceRateRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceToggleLikeRequest;
import com.oriole.wisepen.resource.domain.entity.ResourceUserInteractRecordEntity;
import com.oriole.wisepen.resource.exception.ResourceError;
import com.oriole.wisepen.resource.repository.CustomResourceInteractInfoRepository;
import com.oriole.wisepen.resource.repository.CustomResourceUserInteractRecordRepository;
import com.oriole.wisepen.resource.repository.ResourceItemRepository;
import com.oriole.wisepen.resource.repository.ResourceUserInteractRecordRepository;
import com.oriole.wisepen.resource.service.IResourceInteractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceInteractServiceImpl implements IResourceInteractService {

    private final ResourceItemRepository resourceItemRepository;
    private final ResourceUserInteractRecordRepository resourceUserInteractRecordRepository;
    private final CustomResourceInteractInfoRepository customResourceInteractInfoRepository;
    private final CustomResourceUserInteractRecordRepository customResourceUserInteractRecordRepository;

    /**
     * 点赞/取消点赞。
     */
    @Override
    public void toggleLike(ResourceToggleLikeRequest request) {
        assertResourceExists(request.getResourceId());

        String userId = SecurityContextHolder.getUserId().toString();
        String resourceId = request.getResourceId();

        boolean currentLiked = resourceUserInteractRecordRepository
                .findByUserIdAndResourceId(userId, resourceId)
                .map(r -> Boolean.TRUE.equals(r.getLiked()))
                .orElse(false);
        boolean wantLiked = !currentLiked;

        customResourceUserInteractRecordRepository.findAndSetLiked(resourceId, userId, wantLiked);
        customResourceInteractInfoRepository.incrementLikeCount(resourceId, wantLiked ? 1 : -1);

        log.info("resource like toggled resourceId={} userId={} wantLiked={}",
                resourceId, userId, wantLiked);
    }

    /**
     * 资源评分（1-5），支持覆盖更新。
     * findAndModify(upsert=true, returnNew=false) 将并发请求串行化，保证每个请求获取到的 oldScore 严格一致。
     */
    @Override
    public void rateResource(ResourceRateRequest request) {
        assertResourceExists(request.getResourceId());

        Integer newScore = request.getScore();
        if (newScore == null || newScore < 1 || newScore > 5) {
            throw new ServiceException(ResourceError.SCORE_OUT_OF_RANGE);
        }

        String userId = SecurityContextHolder.getUserId().toString();
        String resourceId = request.getResourceId();

        ResourceUserInteractRecordEntity oldRecord =
                customResourceUserInteractRecordRepository.findAndSetScore(resourceId, userId, newScore);

        Integer oldScore = oldRecord == null ? null : oldRecord.getScore();
        if (oldScore == null) {
            // 首次评分
            customResourceInteractInfoRepository.updateScoreStats(resourceId, 1, newScore);
        } else if (!oldScore.equals(newScore)) {
            // 覆盖评分：scoreCount 不变，仅增量调整总分
            customResourceInteractInfoRepository.updateScoreStats(resourceId, 0, newScore - oldScore);
        }
        // oldScore == newScore：重复提交相同分数，幂等
        log.info("resource rated resourceId={} userId={} oldScore={} newScore={}",
                resourceId, userId, oldScore, newScore);
    }

    private void assertResourceExists(String resourceId) {
        if (!resourceItemRepository.existsById(resourceId)) {
            throw new ServiceException(ResourceError.RESOURCE_NOT_FOUND);
        }
    }
}
