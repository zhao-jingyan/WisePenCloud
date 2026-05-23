package com.oriole.wisepen.common.core.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum IdentityType implements WisePenEnum {

    STUDENT(1, 1, "STUDENT"),
    TEACHER(2, 2, "TEACHER"),
    ADMIN(3, 3, "ADMIN");

    @EnumValue
    private final Integer code;

    private final Integer value;
    private final String desc;

    /**
     * 根据 code 查找枚举
     */
    public static IdentityType getByCode(Integer code) {
        if (code == null) {return null;}
        return Arrays.stream(values())
                .filter(t -> t.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
