package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 集市上架审核状态。
 */
@Getter
@AllArgsConstructor
public enum MarketListingAuditStatus {
    /** 待审核，不可在集市公开展示 */
    PENDING(1, "PENDING"),
    /** 审核通过，可与 LISTED 状态一起在集市展示 */
    APPROVED(2, "APPROVED"),
    /** 审核驳回，不可在集市展示 */
    REJECTED(3, "REJECTED");

    private final int code;

    @EnumValue
    @JsonValue
    private final String value;
}
