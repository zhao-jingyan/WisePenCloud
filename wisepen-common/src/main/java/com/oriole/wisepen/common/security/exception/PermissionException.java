package com.oriole.wisepen.common.security.exception;

import com.oriole.wisepen.common.core.exception.ServiceException;
import lombok.Getter;

@Getter
public class PermissionException extends ServiceException {

    public PermissionException(PermissionError errorCode) {
        super(errorCode);
    }
}