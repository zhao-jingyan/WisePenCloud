package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum SearchScope {
    ALL(1,"ALL"),
    DOCUMENT(2,"DOCUMENT"),
    NOTE(3, "NOTE");

    private final int code;

    @EnumValue
    @JsonValue
    private final String value;

    /**
     * 将 scope 展开为具体的 {@link ResourceType} 列表。
     */
    public List<ResourceType> includedResourceTypes() {
        return switch (this) {
            case ALL -> Arrays.stream(ResourceType.values())
                    .filter(t -> t != ResourceType.UNKNOWN)
                    .collect(Collectors.toList());
            case DOCUMENT -> List.of(ResourceType.PDF, ResourceType.DOC, ResourceType.DOCX, ResourceType.PPT, ResourceType.PPTX, ResourceType.XLS, ResourceType.XLSX);
            case NOTE -> List.of(ResourceType.NOTE, ResourceType.DRAWIO);
        };
    }
}
