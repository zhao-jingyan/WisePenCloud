package com.oriole.wisepen.resource.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.BusinessType;
import com.oriole.wisepen.common.log.annotation.Log;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.resource.domain.dto.req.ResourceRateRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceLikeRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceReadRequest;
import com.oriole.wisepen.resource.domain.dto.res.ResourceUserInteractionRecordResponse;
import com.oriole.wisepen.resource.service.IResourceInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "资源互动", description = "资源点赞与评分")
@RestController
@RequestMapping("/resource/interaction")
@RequiredArgsConstructor
@CheckLogin
public class ResourceInteractionController {

    private final IResourceInteractionService resourceInteractionService;

    @Operation(summary = "获取指定用户对某资源的交互状态", description = "获取指定用户对某资源的交互状态")
    @GetMapping("/getResourceUserInteractionRecord")
    public R<ResourceUserInteractionRecordResponse> getResourceUserInteractionRecord(@RequestParam String resourceId) {
        String userId = SecurityContextHolder.getUserId().toString();
        return R.ok(resourceInteractionService.getResourceUserInteractionInfo(resourceId, userId));
    }

    @Operation(summary = "对资源进行阅读", description = "对资源进行阅读")
    @Log(title = "资源阅读", businessType = BusinessType.UPDATE)
    @PostMapping("/read")
    public R<Void> changeResourceReadStatus(@Validated @RequestBody ResourceReadRequest request) {
        String userId = SecurityContextHolder.getUserId().toString();
        resourceInteractionService.changeResourceReadStatus(request, userId);
        return R.ok();
    }

    @Operation(summary = "对资源进行点赞/取消点赞", description = "对资源进行点赞/取消点赞（点赞状态反转）")
    @Log(title = "资源点赞", businessType = BusinessType.UPDATE)
    @PostMapping("/toggleLike")
    public R<Void> changeResourceLikeStatus(@Validated @RequestBody ResourceLikeRequest request) {
        String userId = SecurityContextHolder.getUserId().toString();
        resourceInteractionService.changeResourceLikeStatus(request, userId);
        return R.ok();
    }

    @Operation(summary = "对资源进行评分", description = "对资源进行评分")
    @Log(title = "资源评分", businessType = BusinessType.UPDATE)
    @PostMapping("/rate")
    public R<Void> changeResourceScore(@Validated @RequestBody ResourceRateRequest request) {
        String userId = SecurityContextHolder.getUserId().toString();
        resourceInteractionService.changeResourceScore(request, userId);
        return R.ok();
    }
}
