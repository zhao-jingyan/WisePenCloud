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

    void publishOffer(MarketPublishOfferRequest request, Long sellerId, Map<Long, GroupRoleType> groupRoles);

    void offShelfOffer(MarketOffShelfOfferRequest request, Long operatorId, Map<Long, GroupRoleType> groupRoles);

    void auditOffer(MarketAuditOfferRequest request, Long operatorId, Map<Long, GroupRoleType> groupRoles);

    MarketOrderResponse purchase(MarketPurchaseRequest request, Long buyerId, Map<Long, GroupRoleType> groupRoles);

    void fork(String orderId, Long buyerId);

    void compensateFork(String orderId, String forkTaskId);

    PageR<MarketOrderResponse> listMyOrders(String buyerId, int page, int size);
}
