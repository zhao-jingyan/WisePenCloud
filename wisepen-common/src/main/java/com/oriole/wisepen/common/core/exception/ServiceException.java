package com.oriole.wisepen.common.core.exception;

import com.oriole.wisepen.common.core.domain.IResult;
import lombok.Getter;

/**
 * 业务异常：用于在 Service 层中断逻辑并抛出错误码
 */
@Getter
public class ServiceException extends RuntimeException {

    private final IResult errorResult;

    public ServiceException(IResult errorResult) {
        super(errorResult.getMsg());
        this.errorResult = errorResult;
    }

    public ServiceException(IResult errorResult, String msg) {
        super(errorResult.getMsg() + ": " + msg);
        this.errorResult = errorResult;
    }
}