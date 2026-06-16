package com.oriole.wisepen.resource.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.common.core.domain.enums.GroupType;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.resource.domain.GroupTagBind;
import com.oriole.wisepen.resource.domain.MarketOfferInfo;
import com.oriole.wisepen.resource.domain.MarketOfferOptions;
import com.oriole.wisepen.resource.domain.dto.req.MarketAuditOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketPublishOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketOffShelfOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketPurchaseRequest;
import com.oriole.wisepen.resource.domain.dto.res.MarketOrderResponse;
import com.oriole.wisepen.resource.domain.entity.MarketOrderEntity;
import com.oriole.wisepen.resource.domain.entity.ResourceItemEntity;
import com.oriole.wisepen.resource.domain.mq.ResourceForkMessage;
import com.oriole.wisepen.resource.enums.MarketOfferStatus;
import com.oriole.wisepen.resource.enums.MarketPurchaseType;
import com.oriole.wisepen.resource.exception.ResourceError;
import com.oriole.wisepen.resource.repository.MarketOrderRepository;
import com.oriole.wisepen.resource.repository.ResourceItemRepository;
import com.oriole.wisepen.resource.mq.IResourceEventPublisher;
import com.oriole.wisepen.resource.service.IMarketService;
import com.oriole.wisepen.resource.service.IResourceService;
import com.oriole.wisepen.user.api.domain.base.GroupDisplayBase;
import com.oriole.wisepen.user.api.domain.dto.req.WalletSettleCoinTradeRequest;
import com.oriole.wisepen.user.api.feign.RemoteUserService;
import com.oriole.wisepen.user.api.feign.RemoteWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketServiceImpl implements IMarketService {

    private final MarketOrderRepository marketOrderRepository;
    private final ResourceItemRepository resourceItemRepository;
    private final IResourceService resourceService;
    private final IResourceEventPublisher resourceEventPublisher;
    private final RemoteUserService remoteUserService;
    private final RemoteWalletService remoteWalletService;

    @Override
    public void publishOffer(MarketPublishOfferRequest request, Long sellerId, Map<Long, GroupRoleType> groupRoles) {
        // 检验是否为资源拥有者
        ResourceItemEntity resource = resourceItemRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ServiceException(ResourceError.RESOURCE_NOT_FOUND));
        if (!sellerId.toString().equals(resource.getOwnerId())) {
            throw new ServiceException(ResourceError.RESOURCE_PERMISSION_DENIED);
        }

        // 检验是否为集市组
        checkPermission(request.getMarketGroupId(), groupRoles);

        // 检验是否已存在
        GroupTagBind existingMarketBind = resource.getGroupBinds().stream()
                .filter(bind -> request.getMarketGroupId().equals(bind.getGroupId()))
                .findFirst()
                .orElse(null);
        MarketOfferOptions existingOffers = existingMarketBind == null ? null : existingMarketBind.getMarketOffers();
        boolean publishForkOnce = request.getForkOncePrice() != null;
        boolean publishForkUnlimited = request.getForkUnlimitedPrice() != null;
        if (publishForkOnce) {
            MarketOfferInfo forkOnce = existingOffers == null ? null : existingOffers.getForkOnce();
            if (forkOnce != null && forkOnce.getStatus() == MarketOfferStatus.BANNED) {
                throw new ServiceException(ResourceError.MARKET_OFFER_BANNED);
            }
        }
        if (publishForkUnlimited) {
            MarketOfferInfo forkUnlimited = existingOffers == null ? null : existingOffers.getForkUnlimited();
            if (forkUnlimited != null && forkUnlimited.getStatus() == MarketOfferStatus.BANNED) {
                throw new ServiceException(ResourceError.MARKET_OFFER_BANNED);
            }
        }

        resourceService.updateGroupResourceTags(resource, request.getMarketGroupId(), sellerId.toString(), GroupRoleType.MEMBER, request.getTagIds());

        LocalDateTime now = LocalDateTime.now();
        GroupTagBind marketBind = resource.getGroupBinds().stream()
                .filter(bind -> request.getMarketGroupId().equals(bind.getGroupId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND));
        MarketOfferOptions offers = marketBind.getMarketOffers();
        if (offers == null) {
            offers = new MarketOfferOptions();
            marketBind.setMarketOffers(offers);
        }
        if (publishForkOnce) {
            MarketOfferInfo forkOnce = offers.getForkOnce();
            if (forkOnce == null) {
                forkOnce = new MarketOfferInfo();
                offers.setForkOnce(forkOnce);
            }
            forkOnce.setPurchaseType(MarketPurchaseType.FORK_ONCE);
            forkOnce.setPrice(request.getForkOncePrice());
            forkOnce.setOfferVersion(request.getOfferVersion());
            forkOnce.setStatus(MarketOfferStatus.PENDING);
            forkOnce.setSellerId(sellerId.toString());
            forkOnce.setEditAt(now);
            forkOnce.setAuditMessage(null);
            forkOnce.setAuditAt(null);
            forkOnce.setAuditorId(null);
        }
        if (publishForkUnlimited) {
            MarketOfferInfo forkUnlimited = offers.getForkUnlimited();
            if (forkUnlimited == null) {
                forkUnlimited = new MarketOfferInfo();
                offers.setForkUnlimited(forkUnlimited);
            }
            forkUnlimited.setPurchaseType(MarketPurchaseType.FORK_UNLIMITED);
            forkUnlimited.setPrice(request.getForkUnlimitedPrice());
            forkUnlimited.setOfferVersion(request.getOfferVersion());
            forkUnlimited.setStatus(MarketOfferStatus.PENDING);
            forkUnlimited.setSellerId(sellerId.toString());
            forkUnlimited.setEditAt(now);
            forkUnlimited.setAuditMessage(null);
            forkUnlimited.setAuditAt(null);
            forkUnlimited.setAuditorId(null);
        }

        resourceItemRepository.save(resource);
        log.info("market offer submitted. resourceId={} sellerId={} marketGroupId={} forkOnce={} forkUnlimited={}",
                resource.getResourceId(), sellerId, request.getMarketGroupId(), publishForkOnce, publishForkUnlimited);
    }

    @Override
    public void offShelfOffer(MarketOffShelfOfferRequest request, Long operatorId, Map<Long, GroupRoleType> groupRoles) {
        // 检验是否存在
        ResourceItemEntity resource = resourceItemRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ServiceException(ResourceError.RESOURCE_NOT_FOUND));
        GroupTagBind marketBind = resource.getGroupBinds().stream()
                .filter(bind -> request.getMarketGroupId().equals(bind.getGroupId()))
                .findFirst()
                .orElse(null);
        if (marketBind == null) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }
        MarketOfferOptions offers = marketBind.getMarketOffers();
        if (offers == null || (offers.getForkOnce() == null && offers.getForkUnlimited() == null)) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }

        // 检验操作权限
        GroupRoleType marketRole = checkPermission(request.getMarketGroupId(), groupRoles);
        if (!operatorId.toString().equals(resource.getOwnerId()) && marketRole != GroupRoleType.OWNER && marketRole != GroupRoleType.ADMIN) {
            throw new ServiceException(ResourceError.RESOURCE_PERMISSION_DENIED);
        }

        boolean offShelfForkOnce = request.getPurchaseTypes() == null || request.getPurchaseTypes().contains(MarketPurchaseType.FORK_ONCE);
        boolean offShelfForkUnlimited = request.getPurchaseTypes() == null || request.getPurchaseTypes().contains(MarketPurchaseType.FORK_UNLIMITED);
        boolean offShelved = false;
        if (offShelfForkOnce && offers.getForkOnce() != null) {
            if (offers.getForkOnce().getStatus() == MarketOfferStatus.BANNED) {
                throw new ServiceException(ResourceError.MARKET_OFFER_BANNED);
            }
            offers.getForkOnce().setStatus(MarketOfferStatus.OFF_SHELF);
            offShelved = true;
        }
        if (offShelfForkUnlimited && offers.getForkUnlimited() != null) {
            if (offers.getForkUnlimited().getStatus() == MarketOfferStatus.BANNED) {
                throw new ServiceException(ResourceError.MARKET_OFFER_BANNED);
            }
            offers.getForkUnlimited().setStatus(MarketOfferStatus.OFF_SHELF);
            offShelved = true;
        }
        if (!offShelved) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }
        resource = resourceItemRepository.save(resource);
        log.info("market offer off-shelved. resourceId={} operatorId={} marketGroupId={}",
                resource.getResourceId(), operatorId, request.getMarketGroupId());
    }

    @Override
    public void auditOffer(MarketAuditOfferRequest request, Long operatorId, Map<Long, GroupRoleType> groupRoles) {
        if ((request.getStatus() == MarketOfferStatus.REJECTED
                || request.getStatus() == MarketOfferStatus.BANNED)
                && !StringUtils.hasText(request.getAuditMessage())) {
            throw new ServiceException(ResourceError.MARKET_AUDIT_MESSAGE_REQUIRED);
        }

        // 检验是否存在
        ResourceItemEntity resource = resourceItemRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ServiceException(ResourceError.RESOURCE_NOT_FOUND));
        GroupTagBind marketBind = resource.getGroupBinds().stream()
                .filter(bind -> request.getMarketGroupId().equals(bind.getGroupId()))
                .findFirst()
                .orElse(null);
        if (marketBind == null) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }
        MarketOfferOptions offers = marketBind.getMarketOffers();
        if (offers == null || (offers.getForkOnce() == null && offers.getForkUnlimited() == null)) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }

        checkPermission(request.getMarketGroupId(), groupRoles);

        LocalDateTime now = LocalDateTime.now();
        boolean audited = false;
        MarketOfferInfo forkOnce = offers.getForkOnce();
        if (forkOnce != null && forkOnce.getStatus() == MarketOfferStatus.PENDING) {
            forkOnce.setStatus(request.getStatus());
            forkOnce.setAuditMessage(request.getAuditMessage());
            forkOnce.setAuditAt(now);
            forkOnce.setAuditorId(operatorId.toString());
            audited = true;
        }
        MarketOfferInfo forkUnlimited = offers.getForkUnlimited();
        if (forkUnlimited != null && forkUnlimited.getStatus() == MarketOfferStatus.PENDING) {
            forkUnlimited.setStatus(request.getStatus());
            forkUnlimited.setAuditMessage(request.getAuditMessage());
            forkUnlimited.setAuditAt(now);
            forkUnlimited.setAuditorId(operatorId.toString());
            audited = true;
        }
        if (!audited) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }
        resourceItemRepository.save(resource);
        log.info("market offer audited. resourceId={} operatorId={} marketGroupId={} status={}",
                resource.getResourceId(), operatorId, request.getMarketGroupId(), request.getStatus());
    }

    @Override
    public MarketOrderResponse purchase(MarketPurchaseRequest request, Long buyerId, Map<Long, GroupRoleType> groupRoles) {
        // 检验是否已上架
        ResourceItemEntity resource = resourceItemRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ServiceException(ResourceError.RESOURCE_NOT_FOUND));
        GroupTagBind marketBind = resource.getGroupBinds().stream()
                .filter(bind -> request.getMarketGroupId().equals(bind.getGroupId()))
                .findFirst()
                .orElse(null);
        if (marketBind == null) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }
        if (marketBind.getTagIds() == null || marketBind.getTagIds().isEmpty()) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_ACTIVE);
        }
        MarketOfferOptions offers = marketBind.getMarketOffers();
        if (offers == null || (offers.getForkOnce() == null && offers.getForkUnlimited() == null)) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }
        MarketOfferInfo offer;
        if (request.getPurchaseType() == MarketPurchaseType.FORK_ONCE) {
            offer = offers.getForkOnce();
        } else if (request.getPurchaseType() == MarketPurchaseType.FORK_UNLIMITED) {
            offer = offers.getForkUnlimited();
        } else {
            throw new ServiceException(ResourceError.MARKET_PURCHASE_TYPE_INVALID);
        }
        if (offer == null) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }
        if (offer.getStatus() != MarketOfferStatus.PUBLISHED) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_ACTIVE);
        }
        if (buyerId.toString().equals(offer.getSellerId())) {
            throw new ServiceException(ResourceError.MARKET_SELF_ORDER_NOT_ALLOWED);
        }

        checkPermission(request.getMarketGroupId(), groupRoles);

        String traceId = "market:" + resource.getResourceId() + ":" + request.getMarketGroupId() + ":" + request.getPurchaseType() + ":" + buyerId;
        MarketOrderEntity existing = marketOrderRepository.findByTradeTraceId(traceId).orElse(null);
        if (existing != null) {
            // TODO：补差价购买方式
            throw new ServiceException(ResourceError.MARKET_ORDER_ALREADY_EXISTS);
        }

        Integer paidPrice = offer.getPrice();
        WalletSettleCoinTradeRequest tradeRequest = WalletSettleCoinTradeRequest.builder()
                .traceId(traceId)
                .buyerId(buyerId)
                .sellerId(Long.valueOf(offer.getSellerId()))
                .price(paidPrice)
                .meta("market resource " + resource.getResourceId() + " " + request.getPurchaseType())
                .build();
        remoteWalletService.settleCoinTrade(tradeRequest);

        MarketOrderEntity order = BeanUtil.copyProperties(resource, MarketOrderEntity.class);
        BeanUtil.copyProperties(offer, order);
        order.setSourceResourceId(resource.getResourceId());
        order.setMarketGroupId(request.getMarketGroupId());
        order.setBuyerId(buyerId.toString());
        order.setPurchaseType(request.getPurchaseType());
        order.setPaidPrice(paidPrice);
        order.setPurchasedOfferVersion(offer.getOfferVersion());
        order.setForkCount(0);
        order.setTradeTraceId(traceId);
        MarketOrderEntity saved = marketOrderRepository.save(order);
        fork(saved.getOrderId(), buyerId);
        saved = marketOrderRepository.findById(saved.getOrderId()).orElse(saved);
        log.info("market order created. orderId={} resourceId={} marketGroupId={} buyerId={} purchaseType={} forkCount={}",
                saved.getOrderId(), resource.getResourceId(), request.getMarketGroupId(), buyerId, request.getPurchaseType(), saved.getForkCount());
        return BeanUtil.copyProperties(saved, MarketOrderResponse.class);
    }

    @Override
    public void fork(String orderId, Long buyerId) {
        MarketOrderEntity order = marketOrderRepository.findById(orderId)
                .orElseThrow(() -> new ServiceException(ResourceError.MARKET_ORDER_NOT_FOUND));
        if (!buyerId.toString().equals(order.getBuyerId())) {
            throw new ServiceException(ResourceError.RESOURCE_PERMISSION_DENIED);
        }

        // 检验是否可 fork
        ResourceItemEntity source = resourceItemRepository.findById(order.getSourceResourceId())
                .orElseThrow(() -> new ServiceException(ResourceError.RESOURCE_NOT_FOUND));
        String marketGroupId = order.getMarketGroupId();
        GroupTagBind marketBind = source.getGroupBinds().stream()
                .filter(bind -> marketGroupId.equals(bind.getGroupId()))
                .findFirst()
                .orElse(null);
        if (marketBind == null) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }
        if (marketBind.getTagIds() == null || marketBind.getTagIds().isEmpty()) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_ACTIVE);
        }
        MarketOfferOptions offers = marketBind.getMarketOffers();
        if (offers == null || (offers.getForkOnce() == null && offers.getForkUnlimited() == null)) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }
        MarketOfferInfo offer;
        if (order.getPurchaseType() == MarketPurchaseType.FORK_ONCE) {
            offer = offers.getForkOnce();
        } else if (order.getPurchaseType() == MarketPurchaseType.FORK_UNLIMITED) {
            offer = offers.getForkUnlimited();
        } else {
            throw new ServiceException(ResourceError.MARKET_PURCHASE_TYPE_INVALID);
        }
        if (offer == null) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }
        if (offer.getStatus() == MarketOfferStatus.BANNED) {
            throw new ServiceException(ResourceError.MARKET_OFFER_BANNED);
        }
        if (offer.getStatus() != MarketOfferStatus.PUBLISHED) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_ACTIVE);
        }
        if (order.getPurchaseType() == MarketPurchaseType.FORK_ONCE && order.getForkCount() >= 1) {
            throw new ServiceException(ResourceError.MARKET_FORK_QUOTA_EXHAUSTED);
        }
        order.setForkCount(order.getForkCount() + 1);
        order = marketOrderRepository.save(order);

        String forkTaskId = IdUtil.fastSimpleUUID();
        ResourceForkMessage forkMessage = ResourceForkMessage.builder()
                .forkTaskId(forkTaskId)
                .orderId(order.getOrderId())
                .sourceResourceId(order.getSourceResourceId())
                .resourceType(source.getResourceType())
                .purchaseType(order.getPurchaseType())
                .version(offer.getOfferVersion())
                .buyerId(buyerId)
                .resourceName(source.getResourceName())
                .preview(source.getPreview())
                .size(source.getSize())
                .build();
        resourceEventPublisher.publishResourceForkEvent(forkMessage);
        log.info("market fork published. orderId={} forkTaskId={} sourceResourceId={} marketGroupId={} purchaseType={} version={} forkCount={}",
                order.getOrderId(), forkTaskId, order.getSourceResourceId(), marketGroupId, order.getPurchaseType(), offer.getOfferVersion(), order.getForkCount());
    }

    @Override
    public void compensateFork(String orderId, String forkTaskId) {
        MarketOrderEntity order = marketOrderRepository.findById(orderId)
                .orElseThrow(() -> new ServiceException(ResourceError.MARKET_ORDER_NOT_FOUND));
        int forkCount = order.getForkCount() == null ? 0 : order.getForkCount();
        order.setForkCount(Math.max(forkCount - 1, 0));
        marketOrderRepository.save(order);
        log.warn("market fork compensated. orderId={} forkTaskId={} forkCount={}",
                orderId, forkTaskId, order.getForkCount());
    }

    @Override
    public PageR<MarketOrderResponse> listMyOrders(String buyerId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        Page<MarketOrderEntity> entityPage = marketOrderRepository.findByBuyerId(buyerId, pageable);
        PageR<MarketOrderResponse> pageR = new PageR<>(entityPage.getTotalElements(), page, size);
        pageR.addAll(entityPage.getContent().stream()
                .map(entity -> BeanUtil.copyProperties(entity, MarketOrderResponse.class))
                .toList());
        return pageR;
    }

    private GroupRoleType checkPermission(String marketGroupId, Map<Long, GroupRoleType> groupRoles) {
        Long marketGroupIdValue = Long.valueOf(marketGroupId);
        Map<Long, GroupDisplayBase> groupMap = remoteUserService.getGroupDisplayInfo(List.of(marketGroupIdValue)).getData();
        GroupDisplayBase groupInfo = groupMap == null ? null : groupMap.get(marketGroupIdValue);
        if (groupInfo == null || groupInfo.getGroupType() != GroupType.MARKET_GROUP) {
            throw new ServiceException(ResourceError.MARKET_GROUP_REQUIRED);
        }
        return groupRoles == null ? null : groupRoles.get(marketGroupIdValue);
    }
}
