package com.oriole.wisepen.resource.service;

import com.oriole.wisepen.resource.domain.dto.req.ResourceRateRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceToggleLikeRequest;

public interface IResourceInteractService {
    void toggleLike(ResourceToggleLikeRequest request);

    void rateResource(ResourceRateRequest request);
}
