package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResourceSortBy {
    UPDATE_TIME(1, "UPDATE_TIME", "updateTime"),
    CREATE_TIME(2, "CREATE_TIME", "createTime"),
    NAME(3, "NAME", "resourceName"),
    SIZE(4, "SIZE", "size");

    @EnumValue
    @JsonValue
    private final int code;

    private final String value;

    private final String dbField;
}
