package com.oriole.wisepen.resource.domain;

import com.oriole.wisepen.resource.enums.MarketListingAuditStatus;
import com.oriole.wisepen.resource.enums.MarketListingStatus;
import com.oriole.wisepen.resource.enums.MarketSellMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 嵌入 {@link com.oriole.wisepen.resource.domain.entity.ResourceItemEntity#listingInfos} 的单条集市售卖信息。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ListingInfo {

    /** 上架记录 ID，同资源内唯一，购买与下架时引用 */
    private String listingId;

    /** 售卖方式，当前仅支持 Fork */
    private MarketSellMethod sellMethod;

    /** 售价（平台币），须 &gt; 0 */
    private Integer price;

    /** 上架审核状态 */
    private MarketListingAuditStatus auditStatus;

    /** 审核说明（驳回原因、管理员备注等） */
    private String auditMessage;

    /** 审核完成时间 */
    private LocalDateTime auditedAt;

    /** 审核人用户 ID */
    private String auditorId;

    /**
     * 上架版本：Fork 时复制的源资源版本。
     * Note 支持非 0 版本；其他资源类型暂为 0。
     */
    private Long listedVersion;

    /** 上架生命周期状态 */
    private MarketListingStatus status;

    /** 上架修订号，每次重新上架或变更售卖信息时递增，用于购买幂等 */
    private Integer revision;

    /** 卖家用户 ID（通常与资源 ownerId 一致） */
    private String sellerId;

    /** 最近一次发起上架的时间 */
    private LocalDateTime listedAt;

    /** 最近一次下架时间 */
    private LocalDateTime offShelfAt;

    public boolean isMarketVisible() {
        return status == MarketListingStatus.LISTED && auditStatus == MarketListingAuditStatus.APPROVED;
    }
}
