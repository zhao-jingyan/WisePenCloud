package com.oriole.wisepen.resource.domain.dto.res;

import com.oriole.wisepen.resource.enums.MarketListingAuditStatus;
import com.oriole.wisepen.resource.enums.MarketListingStatus;
import com.oriole.wisepen.resource.enums.MarketSellMethod;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ListingInfoResponse {

    private String listingId;
    private MarketSellMethod sellMethod;
    private Integer price;
    private MarketListingAuditStatus auditStatus;
    private String auditMessage;
    private LocalDateTime auditedAt;
    private String auditorId;
    private Long listedVersion;
    private MarketListingStatus status;
    private Integer revision;
    private String sellerId;
    private LocalDateTime listedAt;
    private LocalDateTime offShelfAt;
}
