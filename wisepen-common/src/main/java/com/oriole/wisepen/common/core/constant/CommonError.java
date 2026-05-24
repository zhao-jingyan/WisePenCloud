package com.oriole.wisepen.common.core.constant;

import com.oriole.wisepen.common.core.domain.IResult;
import com.oriole.wisepen.common.core.domain.ResultKey;
import com.oriole.wisepen.common.core.domain.enums.BusinessDomain;
import com.oriole.wisepen.common.core.exception.ErrorReason;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
// 1:FRAMEWORK
public enum CommonError implements IResult {

    // 0:SYSTEM
    INTERNAL_ERROR(1000, new ResultKey(BusinessDomain.FRAMEWORK, CommonSubject.SYSTEM, ErrorReason.INTERNAL_ERROR), "系统错误"),
    REQUEST_ERROR(1001, new ResultKey(BusinessDomain.FRAMEWORK, CommonSubject.REQUEST, ErrorReason.INTERNAL_ERROR), "请求错误"),


    // 1:PARAMETER_VALIDATOR
    REQUEST_PARAM_INVALID(1111, new ResultKey(BusinessDomain.FRAMEWORK, CommonSubject.REQUEST_PARAMETER_VALIDATOR, ErrorReason.INVALID), null),
    REQUEST_PARAM_REQUIRED_MISSING(1121, new ResultKey(BusinessDomain.FRAMEWORK, CommonSubject.REQUEST_PARAMETER_VALIDATOR, ErrorReason.REQUIRED_MISSING), null),
    REQUEST_PARAM_ABOVE_UPPER_BOUND(1131, new ResultKey(BusinessDomain.FRAMEWORK, CommonSubject.REQUEST_PARAMETER_VALIDATOR, ErrorReason.ABOVE_UPPER_BOUND), null),
    REQUEST_PARAM_BELOW_LOWER_BOUND(1141, new ResultKey(BusinessDomain.FRAMEWORK, CommonSubject.REQUEST_PARAMETER_VALIDATOR, ErrorReason.BELOW_LOWER_BOUND), null),

    // 2:SECURITY

    // 3:SECURITY_CONTEXT
    SECURITY_CONTEXT_PARAM_MISSING(1311, new ResultKey(BusinessDomain.FRAMEWORK, CommonSubject.SECURITY_CONTEXT, ErrorReason.REQUIRED_MISSING), "安全上下文参数错误");

    private final Integer code;
    private final ResultKey key;
    private final String msg;
}