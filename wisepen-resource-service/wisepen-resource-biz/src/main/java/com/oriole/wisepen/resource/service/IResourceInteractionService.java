package com.oriole.wisepen.resource.service;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.BusinessType;
import com.oriole.wisepen.common.log.annotation.Log;
import com.oriole.wisepen.resource.domain.dto.req.ResourceRateRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceLikeRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceReadRequest;
import com.oriole.wisepen.resource.domain.dto.res.ResourceUserInteractionRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface IResourceInteractionService {

    ResourceUserInteractionRecordResponse getResourceUserInteractionInfo(String resourceId, String userId);

    void changeResourceReadStatus(ResourceReadRequest request, String userId);

    void changeResourceLikeStatus(ResourceLikeRequest request, String userId);

    void changeResourceScore(ResourceRateRequest request, String userId);
}