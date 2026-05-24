package com.oriole.wisepen.note.api.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VersionType {
    FULL(1, "FULL"),
    DELTA(2, "DELTA");

    @EnumValue
    @JsonValue
    private final int code;

    private final String value;
}
