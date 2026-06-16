package com.oriole.wisepen.resource.domain.dto.res;

import lombok.Data;

@Data
public class MarketOfferOptionsResponse {
    private MarketOfferInfoResponse forkOnce;
    private MarketOfferInfoResponse forkUnlimited;
}