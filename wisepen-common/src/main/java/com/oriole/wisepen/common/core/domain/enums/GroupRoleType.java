package com.oriole.wisepen.common.core.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum GroupRoleType implements WisePenEnum {

    OWNER(0, 0, "OWNER"),
    ADMIN(1, 1, "ADMIN"),
    MEMBER(2, 2, "MEMBER"),
    NOT_MEMBER(-1, -1, "NOT_MEMBER");

    @EnumValue
    private final Integer code;

    private final Integer value;
    private final String desc;

    public static GroupRoleType getByCode(Integer code) {
        if (code == null) {return null;}
        return Arrays.stream(values())
                .filter(t -> t.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
