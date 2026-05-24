package com.oriole.wisepen.common.core.constant;

import com.oriole.wisepen.common.core.domain.IBusinessSubject;

import java.util.Locale;

public enum CommonSubject implements IBusinessSubject {
    SYSTEM,
    REQUEST,
    REQUEST_PARAMETER_VALIDATOR,
    SECURITY,
    SECURITY_CONTEXT;

    @Override
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
