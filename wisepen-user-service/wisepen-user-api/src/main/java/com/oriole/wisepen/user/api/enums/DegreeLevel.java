package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DegreeLevel implements WisePenEnum {

    UNKNOWN(0, 0, "UNKNOWN"),
    UNDERGRADUATE(1, 1, "UNDERGRADUATE"),
    MASTER(2, 2, "MASTER"),
    DOCTOR(3, 3, "DOCTOR");

    @EnumValue
    private final Integer code;

    private final Integer value;
    private final String desc;
}
