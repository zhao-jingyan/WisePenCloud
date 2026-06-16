package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MarketOfferStatus {
    /** 已提交上架，待审核，不可购买 */
    PENDING(1, "PENDING"),
    /** 审核通过并上架，可购买 */
    PUBLISHED(2, "PUBLISHED"),
    /** 审核驳回，不可购买 */
    REJECTED(3, "REJECTED"),
    /** 审核封禁，不可再次上架 */
    BANNED(4, "BANNED"),
    /** 已下架，不可购买 */
    OFF_SHELF(5, "OFF_SHELF");

    private final int code;

    @EnumValue
    @JsonValue
    private final String value;
}
