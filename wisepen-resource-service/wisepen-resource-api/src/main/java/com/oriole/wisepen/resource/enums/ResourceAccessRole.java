package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResourceAccessRole {
    OWNER(4, "OWNER"),
    OWNER_SPECIFIED(3, "OWNER_SPECIFIED"),
    GROUP_ADMIN(2, "GROUP_ADMIN"),
    GROUP_MEMBER(1, "GROUP_MEMBER"),
    NONE(0, "NONE");

    @EnumValue
    @JsonValue
    private final int code;

    private final String value;
}
