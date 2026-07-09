package com.oriole.wisepen.ai.asset.constant;

import com.oriole.wisepen.resource.enums.ResourceType;

import java.util.Set;

public interface AIAssetConstants {
    public static final Set<ResourceType> ALLOWED_TYPES = Set.of(
            ResourceType.SKILL,
            ResourceType.AGENT
    );
    public static final long ASSET_STS_TOKEN_DURATION_SECONDS = 3600L;
}
