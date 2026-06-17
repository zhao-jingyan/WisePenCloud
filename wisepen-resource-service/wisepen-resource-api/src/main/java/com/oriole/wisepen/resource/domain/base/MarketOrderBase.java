package com.oriole.wisepen.resource.domain.base;

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
    private String traceId;

    private String buyerId;
    private String sellerId;

    private String purchasedResourceId;
    private Integer purchasedOfferVersion;
    private String marketGroupId;

    private Integer buyerGrantedActionsMask;
    private Integer buyerPaidPrice;
}
