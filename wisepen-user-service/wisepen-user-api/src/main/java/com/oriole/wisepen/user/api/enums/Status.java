package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status implements WisePenEnum {

    NORMAL(1, 1, "NORMAL"),
    UNIDENTIFIED(-1, -1, "UNIDENTIFIED"),
    BANNED(-2, -2, "BANNED");

    @EnumValue
    private final Integer code;

    private final Integer value;
    private final String desc;
}
