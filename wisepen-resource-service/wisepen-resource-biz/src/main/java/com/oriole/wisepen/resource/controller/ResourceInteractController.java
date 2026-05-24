package com.oriole.wisepen.resource.controller;

import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.BusinessType;
import com.oriole.wisepen.common.log.annotation.Log;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.resource.domain.dto.req.ResourceRateRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceToggleLikeRequest;
import com.oriole.wisepen.resource.service.IResourceInteractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "资源互动", description = "资源点赞与评分")
@RestController
@RequestMapping("/resource/interact")
@RequiredArgsConstructor
@CheckLogin
public class ResourceInteractController {

    private final IResourceInteractService resourceInteractService;

    @Operation(summary = "点赞/取消点赞", description = "同一用户对同一资源的点赞状态可反转")
    @Log(title = "资源点赞", businessType = BusinessType.UPDATE)
    @PostMapping("/toggleLike")
    public R<Void> toggleLike(@Validated @RequestBody ResourceToggleLikeRequest request) {
        resourceInteractService.toggleLike(request);
        return R.ok();
    }

    @Operation(summary = "资源评分", description = "对资源进行1-5分评分，支持覆盖更新")
    @Log(title = "资源评分", businessType = BusinessType.UPDATE)
    @PostMapping("/rate")
    public R<Void> rate(@Validated @RequestBody ResourceRateRequest request) {
        resourceInteractService.rateResource(request);
        return R.ok();
    }
}
