package com.oriole.wisepen.common.core.domain.enums.list;

import com.fasterxml.jackson.annotation.JsonValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;

//通用查询逻辑枚举 (用于控制多个条件传入时的组合策略)
public enum QueryLogicEnum implements WisePenEnum {
    // 且关系：必须同时满足所有传入的条件 (交集)
    AND(1, "AND", "AND"),
    // 或关系：满足传入的任意一个条件即可 (并集)
    OR(2, "OR", "OR");

    private final Integer code;
    private final String value;
    private final String desc;

    QueryLogicEnum(Integer code, String value, String desc) {
        this.code = code;
        this.value = value;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
