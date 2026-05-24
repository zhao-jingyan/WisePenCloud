package com.oriole.wisepen.document.api.constant;

import com.oriole.wisepen.common.core.domain.IBusinessSubject;

import java.util.Locale;

public enum DocumentSubject implements IBusinessSubject {
    DOCUMENT,
    DOCUMENT_PREVIEW,
    DOCUMENT_PROCESS;

    @Override
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}