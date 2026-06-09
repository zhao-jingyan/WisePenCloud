package com.oriole.wisepen.system.service;

import com.oriole.wisepen.system.api.domain.dto.req.FeedbackRequest;

public interface FeedbackService {
    void createFeedback(Long userId, FeedbackRequest feedbackRequest);
}
