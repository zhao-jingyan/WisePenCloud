package com.oriole.wisepen.common.core.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 面向接口传输的枚举。
 * code 是稳定数字码，value 是接口传输值，desc 是稳定语义描述。
 */
public interface WisePenEnum {

    Integer getCode();

    @JsonValue
    Object getValue();

    String getDesc();

    default String getKey() {
        return ((Enum<?>) this).name();
    }
}
