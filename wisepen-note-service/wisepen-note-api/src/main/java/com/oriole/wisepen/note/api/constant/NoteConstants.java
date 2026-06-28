package com.oriole.wisepen.note.api.constant;

import com.oriole.wisepen.resource.enums.ResourceType;

import java.util.Set;

public class NoteConstants {
    /** 本服务允许处理的文件类型白名单 */
    public static final Set<ResourceType> ALLOWED_TYPES = Set.of(
            ResourceType.NOTE,
            ResourceType.DRAWIO
    );
}
