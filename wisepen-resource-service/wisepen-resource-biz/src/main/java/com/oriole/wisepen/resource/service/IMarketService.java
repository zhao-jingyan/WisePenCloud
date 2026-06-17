package com.oriole.wisepen.resource.service;

import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.resource.domain.dto.req.MarketAuditOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketPublishOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketOffShelfOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketPurchaseRequest;
import com.oriole.wisepen.resource.domain.dto.res.MarketOrderResponse;

import java.util.Map;

public interface IMarketService {

    void publishOffer(MarketPublishOfferRequest request);

    void offShelfOffer(MarketOffShelfOfferRequest request);

    void auditOffer(MarketAuditOfferRequest request, String operatorId);

    MarketOrderResponse purchase(MarketPurchaseRequest request, String buyerId);

    PageR<MarketOrderResponse> listOrders(String buyerId, int page, int size);
}
