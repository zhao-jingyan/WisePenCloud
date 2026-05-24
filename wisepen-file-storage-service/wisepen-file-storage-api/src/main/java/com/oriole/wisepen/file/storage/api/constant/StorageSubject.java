package com.oriole.wisepen.file.storage.api.constant;

import com.oriole.wisepen.common.core.domain.IBusinessSubject;

import java.util.Locale;

public enum StorageSubject implements IBusinessSubject {
    STORAGE_PROVIDER,
    FILE;

    @Override
    public String key() {
        return name().toLowerCase(
                Locale.ROOT);
    }
}
