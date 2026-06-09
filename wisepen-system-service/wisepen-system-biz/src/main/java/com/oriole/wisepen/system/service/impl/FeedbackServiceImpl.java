package com.oriole.wisepen.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.oriole.wisepen.system.api.domain.dto.req.FeedbackRequest;
import com.oriole.wisepen.system.api.enums.FeedbackStatus;
import com.oriole.wisepen.system.domain.entity.FeedbackEntity;
import com.oriole.wisepen.system.mapper.FeedbackMapper;
import com.oriole.wisepen.system.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Xiong Heng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackMapper feedbackMapper;

    @Override
    public void createFeedback(Long userId, FeedbackRequest feedbackRequest) {
        FeedbackEntity feedbackEntity = BeanUtil.copyProperties(feedbackRequest, FeedbackEntity.class);
        // 业务默认值赋值
        feedbackEntity.setStatus(FeedbackStatus.PENDING);
        feedbackMapper.insert(feedbackEntity);
    }
}