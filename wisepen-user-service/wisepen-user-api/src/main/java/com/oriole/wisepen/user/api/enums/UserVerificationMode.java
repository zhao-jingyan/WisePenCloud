package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserVerificationMode {
    EDU_EMAIL(1, "EDU_EMAIL"),
    FDU_UIS_SYS(2, "FDU_UIS_SYS");

    @EnumValue
    @JsonValue
    private final int code;

    private final String value;
}
