package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 集市购买权益类型，由买家下单时选择。
 */
@Getter
@AllArgsConstructor
public enum MarketPurchaseType {
    /** 购买后立即 Fork 一次当前上架版本 */
    FORK_ONCE(1, "FORK_ONCE"),
    /** 购买后立即 Fork 一次，之后可继续 Fork 当前上架版本 */
    FORK_UNLIMITED(2, "FORK_UNLIMITED");

    private final int code;

    @EnumValue
    @JsonValue
    private final String value;
}
