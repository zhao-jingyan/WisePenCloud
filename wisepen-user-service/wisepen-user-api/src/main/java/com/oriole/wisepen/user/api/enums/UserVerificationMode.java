package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserVerificationMode implements WisePenEnum {
    EDU_EMAIL(1, "EDU_EMAIL", "EDU_EMAIL"),
    FDU_UIS_SYS(2, "FDU_UIS_SYS", "FDU_UIS_SYS");

    @EnumValue
    private final Integer code;
    private final String value;
    private final String desc;
}
