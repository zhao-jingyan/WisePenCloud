package com.oriole.wisepen.extension.fudan.controller;

import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.extension.fudan.cache.RedisCacheManager;
import com.oriole.wisepen.extension.fudan.domain.dto.FudanUISTaskResultDTO;
import com.oriole.wisepen.extension.fudan.exception.FudanExtensionError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "内部 - 复旦 UIS 认证", description = "供用户服务查询复旦 UIS 认证任务状态")
@RestController
@RequestMapping("/internal/extenion/fudan/uis")
@RequiredArgsConstructor
public class UISTaskController {

    private final RedisCacheManager redisCacheManager;

    @Operation(
            summary = "内部获取复旦 UIS 认证状态",
            description = """
                    - 用途：供用户服务查询指定用户的复旦 UIS 身份认证任务结果。
                    - 请求：userId 指定发起 UIS 认证的用户。
                    - 约束：调用方必须通过内部服务调用边界；任务状态必须仍存在于缓存中。
                    - 处理：从 Redis 读取用户 UIS 任务结果；不主动发起新的 UIS 认证，也不修改用户认证状态。
                    - 失败：任务不存在或已过期 -> FudanExtensionError.UIS_TASK_NOT_FOUND；缓存读取发生未处理异常 -> CommonError.INTERNAL_ERROR。
                    - 响应：返回 UIS 任务结果和状态信息。
                    """
    )
    @GetMapping("/getUISVerificationStatus")
    public R<FudanUISTaskResultDTO> getTaskStatus(@RequestParam Long userId) {
        FudanUISTaskResultDTO result = redisCacheManager.getUisTaskStatus(userId);

        if (result == null) {
            throw new ServiceException(FudanExtensionError.UIS_TASK_NOT_FOUND);
        }
        return R.ok(result);
    }
}
