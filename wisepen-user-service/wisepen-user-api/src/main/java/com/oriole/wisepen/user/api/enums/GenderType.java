package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GenderType implements WisePenEnum {

    MALE(0, 0, "MALE"),
    FEMALE(1, 1, "FEMALE"),
    UNKNOWN(2, 2, "UNKNOWN");

    @EnumValue
    private final Integer code;

    private final Integer value;
    private final String desc;
}
