package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenTransferType implements WisePenEnum {
    GROUP_INFLOW(1, 1, "GROUP_INFLOW"),
    USER_INFLOW(2, 2, "USER_INFLOW");

    @EnumValue
    private final Integer code;

    private final Integer value;
    private final String desc;
}
