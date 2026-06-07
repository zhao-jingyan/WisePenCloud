package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 集市售卖方式。买家完成交易后对资源的处置方式。
 */
@Getter
@AllArgsConstructor
public enum MarketSellMethod {
    /** 购买后可 Fork 副本到买家个人空间 */
    FORK(1, "FORK");

    private final int code;

    @EnumValue
    @JsonValue
    private final String value;
}
