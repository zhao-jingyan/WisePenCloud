package com.oriole.wisepen.resource.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketOfferOptions {
    private MarketOfferInfo forkOnce;
    private MarketOfferInfo forkUnlimited;
}