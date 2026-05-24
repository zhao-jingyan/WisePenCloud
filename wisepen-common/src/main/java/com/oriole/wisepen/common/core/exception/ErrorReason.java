package com.oriole.wisepen.common.core.exception;

import com.oriole.wisepen.common.core.domain.IResultOutcome;

import java.util.Locale;

public enum ErrorReason implements IResultOutcome {

    // 业务错误
    INVALID(ErrorReasonCategory.BUSINESS), // 值非法（格式）
    REQUIRED_MISSING(ErrorReasonCategory.BUSINESS), // 必填项缺失
    NOT_FOUND(ErrorReasonCategory.BUSINESS), // 记录未找到
    ALREADY_EXISTS(ErrorReasonCategory.BUSINESS), // 记录已存在
    CONFLICT(ErrorReasonCategory.BUSINESS), // 冲突
    STATE_INVALID(ErrorReasonCategory.BUSINESS), // 非法操作（状态非法，对象当前状态不允许）
    NOT_ALLOWED(ErrorReasonCategory.BUSINESS), // 非法操作（能力支持但规则禁止）
    UNSUPPORTED(ErrorReasonCategory.BUSINESS), // 非法操作（能力不支持）
    EXPIRED(ErrorReasonCategory.BUSINESS), // 过期
    LOCKED(ErrorReasonCategory.BUSINESS), // 锁定
    ABOVE_UPPER_BOUND(ErrorReasonCategory.BUSINESS), // 超过上限
    BELOW_LOWER_BOUND(ErrorReasonCategory.BUSINESS), // 低于下限

    // 权限错误
    UNAUTHENTICATED(ErrorReasonCategory.SECURITY), // 身份未认证
    UNAUTHORIZED(ErrorReasonCategory.SECURITY), // 角色未满足要求
    PERMISSION_DENIED(ErrorReasonCategory.SECURITY), // 非法操作（权限不足）


    // 外部系统错误
    EXTERNAL_FAILED(ErrorReasonCategory.EXTERNAL), // 外部服务调用失败
    EXTERNAL_TIMEOUT(ErrorReasonCategory.EXTERNAL), // 外部服务调用超时
    EXTERNAL_UNAVAILABLE(ErrorReasonCategory.EXTERNAL), // 外部服务不可用

    // 内部微服务间系统错误
    FAILED(ErrorReasonCategory.SYSTEM), // 操作失败
    TIMEOUT(ErrorReasonCategory.SYSTEM), // 操作超时
    UNAVAILABLE(ErrorReasonCategory.SYSTEM), // 服务不可用
    INTERNAL_ERROR(ErrorReasonCategory.SYSTEM); // 系统内部错误

    public enum ErrorReasonCategory {
        BUSINESS,
        SECURITY,
        SYSTEM,
        EXTERNAL
    }

    private final ErrorReasonCategory category;


    ErrorReason(ErrorReasonCategory category) {
        this.category = category;
    }

    public ErrorReasonCategory category() {
        return category;
    }

    @Override
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}