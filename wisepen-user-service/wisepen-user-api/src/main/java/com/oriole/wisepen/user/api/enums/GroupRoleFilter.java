package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GroupRoleFilter {
    ALL(0,"ALL"),
    JOINED(1,"JOINED"),
    MANAGED(2,"MANAGED");

    private final int code;

    @EnumValue
    @JsonValue
    private final String value;
}
