package com.oriole.wisepen.system.api.constant;

import com.oriole.wisepen.common.core.domain.IBusinessSubject;

import java.util.Locale;

public enum SystemSubject implements IBusinessSubject {
    MAIL;

    @Override
    public String key() {
        return name().toLowerCase(
                Locale.ROOT);
    }
}
