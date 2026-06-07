package com.oriole.wisepen.resource.service;

import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.resource.domain.dto.req.MarketForkRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketListResourceRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketOffShelfRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketPurchaseRequest;
import com.oriole.wisepen.resource.domain.dto.res.MarketListingResponse;
import com.oriole.wisepen.resource.domain.dto.res.MarketPurchaseResponse;

import java.util.Map;

public interface IMarketService {

    MarketListingResponse addListing(MarketListResourceRequest request, Long sellerId, Map<Long, GroupRoleType> groupRoles);

    void offShelf(MarketOffShelfRequest request, Long operatorId, Map<Long, GroupRoleType> groupRoles);

    MarketPurchaseResponse purchase(MarketPurchaseRequest request, Long buyerId, Map<Long, GroupRoleType> groupRoles);

    MarketPurchaseResponse fork(MarketForkRequest request, Long buyerId);

    PageR<MarketListingResponse> listMyListings(String sellerId, int page, int size);

    PageR<MarketPurchaseResponse> listMyPurchases(String buyerId, int page, int size);
}
