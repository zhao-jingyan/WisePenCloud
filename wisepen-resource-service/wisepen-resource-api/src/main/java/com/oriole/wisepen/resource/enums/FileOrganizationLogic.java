package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileOrganizationLogic implements WisePenEnum {
    FOLDER(1, "FOLDER", "FOLDER"),
    TAG(2, "TAG", "TAG");

    @EnumValue
    private final Integer code;
    private final String value;
    private final String desc;
}
