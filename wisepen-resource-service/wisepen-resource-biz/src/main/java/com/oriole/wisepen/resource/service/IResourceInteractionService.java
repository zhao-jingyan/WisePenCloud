package com.oriole.wisepen.resource.service;

import com.oriole.wisepen.resource.domain.dto.req.ResourceRateRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceLikeRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceReadRequest;
import com.oriole.wisepen.resource.domain.dto.res.ResourceUserInteractionRecordResponse;

public interface IResourceInteractionService {

    ResourceUserInteractionRecordResponse getResourceUserInteractionInfo(String resourceId, String userId);

    void changeResourceReadStatus(ResourceReadRequest request, String userId);

    void changeResourceLikeStatus(ResourceLikeRequest request, String userId);

    void changeResourceScore(ResourceRateRequest request, String userId);
}
