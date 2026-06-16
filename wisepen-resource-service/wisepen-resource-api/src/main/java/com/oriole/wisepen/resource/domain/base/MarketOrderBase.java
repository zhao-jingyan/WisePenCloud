package com.oriole.wisepen.resource.domain.base;

import com.oriole.wisepen.resource.enums.MarketPurchaseType;
import com.oriole.wisepen.resource.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOrderBase {
    private String buyerId;
    private String sellerId;
    private String sourceResourceId;
    private String marketGroupId;
    private MarketPurchaseType purchaseType;
    private Integer paidPrice;
    private Long purchasedOfferVersion;
    private Integer forkCount;
    private String tradeTraceId;
    private ResourceType resourceType;
}
