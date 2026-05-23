package com.oriole.wisepen.common.core.domain.enums.list;

import com.fasterxml.jackson.annotation.JsonValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;

// 通用排序方向枚举
public enum SortDirectionEnum implements WisePenEnum {
    // 正序 (从小到大 / 从旧到新 / A-Z)
    ASC(1, "ASC", "ASC"),
    //倒序 (从大到小 / 从新到旧 / Z-A)
    DESC(2, "DESC", "DESC");

    private final Integer code;
    private final String value;
    private final String desc;

    SortDirectionEnum(Integer code, String value, String desc) {
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

    //转换为 Spring Data 的原生 Direction
    public org.springframework.data.domain.Sort.Direction toSpringDirection() {
        return this == ASC ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
    }
}
