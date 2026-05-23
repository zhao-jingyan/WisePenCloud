package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GroupRoleFilter implements WisePenEnum {
    JOINED(1, 1, "JOINED"),
    MANAGED(2, 2, "MANAGED");
    @EnumValue
    private final Integer code;
    private final Integer value;
    private final String desc;
}
