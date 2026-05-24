package com.oriole.wisepen.resource.constant;

import com.oriole.wisepen.common.core.domain.IBusinessSubject;

import java.util.Locale;

public enum ResourceSubject implements IBusinessSubject {
    TAG_NODE,
    TAG_PATH_NODE,
    TAG_TREE,
    RESOURCE,
    RESOURCE_TAG;

    @Override
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}