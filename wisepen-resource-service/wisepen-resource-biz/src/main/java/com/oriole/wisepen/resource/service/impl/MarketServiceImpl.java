package com.oriole.wisepen.resource.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.common.core.domain.enums.GroupType;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.document.api.feign.RemoteDocumentService;
import com.oriole.wisepen.note.api.feign.RemoteNoteService;
import com.oriole.wisepen.resource.domain.ListingInfo;
import com.oriole.wisepen.resource.domain.dto.ResourceCheckPermissionReqDTO;
import com.oriole.wisepen.resource.domain.dto.ResourceCheckPermissionResDTO;
import com.oriole.wisepen.resource.domain.dto.ResourceCreateReqDTO;
import com.oriole.wisepen.resource.domain.dto.req.MarketForkRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketListResourceRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketOffShelfRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketPurchaseRequest;
import com.oriole.wisepen.resource.domain.dto.req.ResourceForkRequest;
import com.oriole.wisepen.resource.domain.dto.res.MarketListingResponse;
import com.oriole.wisepen.resource.domain.dto.res.MarketPurchaseResponse;
import com.oriole.wisepen.resource.domain.entity.MarketListingEntity;
import com.oriole.wisepen.resource.domain.entity.MarketPurchaseEntity;
import com.oriole.wisepen.resource.domain.entity.ResourceInteractionInfoEntity;
import com.oriole.wisepen.resource.domain.entity.ResourceItemEntity;
import com.oriole.wisepen.resource.domain.entity.TagEntity;
import com.oriole.wisepen.resource.enums.MarketListingAuditStatus;
import com.oriole.wisepen.resource.enums.MarketListingStatus;
import com.oriole.wisepen.resource.enums.MarketSellMethod;
import com.oriole.wisepen.resource.enums.ResourceAccessRole;
import com.oriole.wisepen.resource.enums.ResourceAction;
import com.oriole.wisepen.resource.enums.ResourceType;
import com.oriole.wisepen.resource.exception.ResourceError;
import com.oriole.wisepen.resource.repository.MarketListingRepository;
import com.oriole.wisepen.resource.repository.MarketPurchaseRepository;
import com.oriole.wisepen.resource.repository.ResourceInteractionInfoRepository;
import com.oriole.wisepen.resource.repository.ResourceItemRepository;
import com.oriole.wisepen.resource.repository.TagRepository;
import com.oriole.wisepen.resource.service.IMarketService;
import com.oriole.wisepen.resource.service.IResourceService;
import com.oriole.wisepen.user.api.domain.base.GroupDisplayBase;
import com.oriole.wisepen.user.api.domain.base.UserDisplayBase;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketServiceImpl implements IMarketService {

    private final MarketListingRepository marketListingRepository;
    private final MarketPurchaseRepository marketPurchaseRepository;
    private final ResourceInteractionInfoRepository resourceInteractionInfoRepository;
    private final ResourceItemRepository resourceItemRepository;
    private final TagRepository tagRepository;
    private final IResourceService resourceService;
    private final RemoteUserService remoteUserService;
    private final RemoteWalletService remoteWalletService;
    private final RemoteNoteService remoteNoteService;
    private final RemoteDocumentService remoteDocumentService;

    @Override
    public MarketListingResponse addListing(MarketListResourceRequest request, Long sellerId, Map<Long, GroupRoleType> groupRoles) {
        // 检验是否为资源拥有者
        String sellerIdStr = sellerId.toString();
        resourceService.assertResourceOwner(request.getResourceId(), sellerIdStr);
        ResourceItemEntity resource = resourceItemRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ServiceException(ResourceError.RESOURCE_NOT_FOUND));

        // 检验是否为集市组
        Long marketGroupId = Long.valueOf(request.getMarketGroupId());
        Map<Long, GroupDisplayBase> groupMap = remoteUserService.getGroupDisplayInfo(List.of(marketGroupId)).getData();
        GroupDisplayBase groupInfo = groupMap == null ? null : groupMap.get(marketGroupId);
        if (groupInfo == null || groupInfo.getGroupType() != GroupType.MARKET_GROUP) {
            throw new ServiceException(ResourceError.MARKET_GROUP_REQUIRED);
        }

        // 检验上架权限 or 公开？
        GroupRoleType marketRole = groupRoles == null ? null : groupRoles.get(marketGroupId);
        if (marketRole == null || marketRole == GroupRoleType.NOT_MEMBER) {
            throw new ServiceException(ResourceError.RESOURCE_PERMISSION_DENIED);
        }

        // 检验上架 Tag
        List<TagEntity> tags = tagRepository.findAllById(request.getTagIds());
        if (tags.size() != request.getTagIds().size()
                || tags.stream().anyMatch(tag -> !request.getMarketGroupId().equals(tag.getGroupId()))) {
            throw new ServiceException(ResourceError.TAG_NODE_NOT_FOUND);
        }

        // 检验上架 Version
        Long listedVersion = request.getListedVersion();
        MarketSellMethod sellMethod = request.getSellMethod();
        // TODO: 文档版本管理
        if (resource.getResourceType() != ResourceType.NOTE && listedVersion != 0L) {
            throw new ServiceException(ResourceError.MARKET_VERSION_NOT_SUPPORTED);
        }

        ListingInfo existing = findListingInfo(resource, sellMethod, listedVersion);
        if (existing != null && existing.getStatus() == MarketListingStatus.LISTED) {
            throw new ServiceException(ResourceError.MARKET_LISTING_ALREADY_EXISTS);
        }

        resourceService.updateGroupResourceTags(
                request.getResourceId(),
                request.getMarketGroupId(),
                sellerIdStr,
                marketRole,
                request.getTagIds()
        );

        LocalDateTime now = LocalDateTime.now();

        ListingInfo listing;
        if (existing != null) {
            listing = existing;
            listing.setRevision(existing.getRevision() == null ? 1 : existing.getRevision() + 1);
        } else {
            if (resource.getListingInfos() == null) {
                resource.setListingInfos(new ArrayList<>());
            }
            listing = ListingInfo.builder()
                    .listingId(UUID.randomUUID().toString())
                    .sellMethod(sellMethod)
                    .listedVersion(listedVersion)
                    .revision(1)
                    .build();
            resource.getListingInfos().add(listing);
        }

        listing.setPrice(request.getPrice());
        listing.setStatus(MarketListingStatus.LISTED);
        listing.setSellerId(sellerIdStr);
        listing.setListedAt(now);
        listing.setOffShelfAt(null);
        listing.setAuditStatus(MarketListingAuditStatus.PENDING);
        resourceItemRepository.save(resource);
        log.info("market listing saved listingId={} resourceId={} sellerId={} marketGroupId={} revision={}",
                listing.getListingId(), resource.getResourceId(), sellerIdStr, request.getMarketGroupId(), listing.getRevision());

        MarketListingResponse response = BeanUtil.copyProperties(resource, MarketListingResponse.class);
        BeanUtil.copyProperties(listing, response);
        response.setSourceResourceId(resource.getResourceId());
        response.setMarketGroupId(request.getMarketGroupId());
        response.setTagIds(request.getTagIds());
        response.setCurrentTags(tags.stream().collect(Collectors.toMap(TagEntity::getTagId, TagEntity::getTagName)));
        ResourceInteractionInfoEntity interactionInfo = resourceInteractionInfoRepository.findById(resource.getResourceId())
                .orElseGet(ResourceInteractionInfoEntity::new);
        response.setResourceInteractionInfo(interactionInfo);
        try {
            Long seller = Long.valueOf(listing.getSellerId());
            response.setSellerInfo(remoteUserService.getUserDisplayInfo(List.of(seller)).getData().get(seller));
        } catch (Exception e) {
            log.debug("market seller info degraded sellerId={}", listing.getSellerId(), e);
            response.setSellerInfo(new UserDisplayBase("UNKNOW", null, null, null));
        }
        return response;
    }

    private ListingInfo findListingInfo(ResourceItemEntity resource, MarketSellMethod sellMethod, Long listedVersion) {
        if (resource.getListingInfos() == null || resource.getListingInfos().isEmpty()) {
            return null;
        }
        return resource.getListingInfos().stream()
                .filter(info -> info.getSellMethod() == sellMethod
                        && Objects.equals(info.getListedVersion(), listedVersion))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void offShelf(MarketOffShelfRequest request, Long operatorId, Map<Long, GroupRoleType> groupRoles) {
        MarketListingEntity entity = marketListingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ServiceException(ResourceError.MARKET_LISTING_NOT_FOUND));
        boolean isSeller = operatorId.toString().equals(entity.getSellerId());
        GroupRoleType marketRole = groupRoles == null ? null : groupRoles.get(Long.valueOf(entity.getMarketGroupId()));
        boolean isMarketAdmin = marketRole == GroupRoleType.OWNER || marketRole == GroupRoleType.ADMIN;
        if (!isSeller && !isMarketAdmin) {
            throw new ServiceException(ResourceError.RESOURCE_PERMISSION_DENIED);
        }

        resourceService.updateGroupResourceTags(
                entity.getSourceResourceId(),
                entity.getMarketGroupId(),
                operatorId.toString(),
                marketRole,
                null
        );
        entity.setStatus(MarketListingStatus.OFF_SHELF);
        entity.setRevision(entity.getRevision() == null ? 1 : entity.getRevision() + 1);
        marketListingRepository.save(entity);
        log.info("market listing offShelf listingId={} sourceResourceId={} operatorId={}",
                entity.getListingId(), entity.getSourceResourceId(), operatorId);
    }

    @Override
    public MarketPurchaseResponse purchase(MarketPurchaseRequest request, Long buyerId, Map<Long, GroupRoleType> groupRoles) {
        MarketListingEntity listing = marketListingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ServiceException(ResourceError.MARKET_LISTING_NOT_FOUND));
        if (listing.getStatus() != MarketListingStatus.LISTED) {
            throw new ServiceException(ResourceError.MARKET_LISTING_NOT_ACTIVE);
        }
        if (buyerId.toString().equals(listing.getSellerId())) {
            throw new ServiceException(ResourceError.MARKET_SELF_PURCHASE_NOT_ALLOWED);
        }

        // 检验权限 or 公开？
        GroupRoleType marketRole = groupRoles == null ? null : groupRoles.get(Long.valueOf(listing.getMarketGroupId()));
        if (marketRole == null || marketRole == GroupRoleType.NOT_MEMBER) {
            throw new ServiceException(ResourceError.RESOURCE_PERMISSION_DENIED);
        }
        ResourceCheckPermissionResDTO permission = resourceService.checkPermission(ResourceCheckPermissionReqDTO.builder()
                .resourceId(listing.getSourceResourceId())
                .userId(buyerId)
                .groupRoles(groupRoles)
                .build());
        boolean viewable = permission.getResourceAccessRole() == ResourceAccessRole.OWNER
                || permission.getAllowedActions() != null && permission.getAllowedActions().contains(ResourceAction.VIEW);
        if (!viewable) {
            throw new ServiceException(ResourceError.RESOURCE_PERMISSION_DENIED);
        }

        String traceId = "market:" + listing.getListingId() + ":" + buyerId + ":" + listing.getRevision();
        MarketPurchaseEntity existing = marketPurchaseRepository.findByTradeTraceId(traceId).orElse(null);
        if (existing != null) {
            return BeanUtil.copyProperties(existing, MarketPurchaseResponse.class);
        }

        WalletSettleCoinTradeRequest tradeRequest = BeanUtil.copyProperties(listing, WalletSettleCoinTradeRequest.class);
        tradeRequest.setTraceId(traceId);
        tradeRequest.setBuyerId(buyerId);
        tradeRequest.setSellerId(Long.valueOf(listing.getSellerId()));
        tradeRequest.setMeta("market listing " + listing.getListingId());
        remoteWalletService.settleCoinTrade(tradeRequest);

        MarketPurchaseEntity purchase = BeanUtil.copyProperties(listing, MarketPurchaseEntity.class);
        purchase.setBuyerId(buyerId.toString());
        purchase.setPaidPrice(listing.getPrice());
        purchase.setForkedVersion(listing.getListedVersion() == null ? 0L : listing.getListedVersion());
        purchase.setListingRevision(listing.getRevision());
        purchase.setTradeTraceId(traceId);
        MarketPurchaseEntity saved = marketPurchaseRepository.save(purchase);
        log.info("market purchase saved purchaseId={} listingId={} buyerId={} revision={}",
                saved.getPurchaseId(), listing.getListingId(), buyerId, listing.getRevision());
        return BeanUtil.copyProperties(saved, MarketPurchaseResponse.class);
    }

    @Override
    public MarketPurchaseResponse fork(MarketForkRequest request, Long buyerId) {
        MarketPurchaseEntity purchase = marketPurchaseRepository.findById(request.getPurchaseId())
                .orElseThrow(() -> new ServiceException(ResourceError.MARKET_PURCHASE_NOT_FOUND));
        if (!buyerId.toString().equals(purchase.getBuyerId())) {
            throw new ServiceException(ResourceError.RESOURCE_PERMISSION_DENIED);
        }
        if (StringUtils.hasText(purchase.getForkedResourceId())) {
            return BeanUtil.copyProperties(purchase, MarketPurchaseResponse.class);
        }

        ResourceItemEntity source = resourceItemRepository.findById(purchase.getSourceResourceId())
                .orElseThrow(() -> new ServiceException(ResourceError.RESOURCE_NOT_FOUND));
        ResourceCreateReqDTO createReq = BeanUtil.copyProperties(source, ResourceCreateReqDTO.class);
        createReq.setOwnerId(buyerId.toString());
        createReq.setPathTagId(request.getPathTagId());
        String forkedResourceId = resourceService.createResourceItem(createReq);

        try {
            Long forkedVersion = purchase.getForkedVersion() == null ? 0L : purchase.getForkedVersion();
            ResourceForkRequest forkRequest = ResourceForkRequest.builder()
                    .sourceResourceId(source.getResourceId())
                    .targetResourceId(forkedResourceId)
                    .version(forkedVersion)
                    .buyerId(buyerId)
                    .build();
            if (source.getResourceType() == ResourceType.NOTE) {
                remoteNoteService.forkNote(forkRequest);
            } else if (Set.of(ResourceType.PDF, ResourceType.DOC, ResourceType.DOCX, ResourceType.PPT,
                    ResourceType.PPTX, ResourceType.XLS, ResourceType.XLSX).contains(source.getResourceType())) {
                remoteDocumentService.forkDocument(forkRequest);
            } else {
                throw new ServiceException(ResourceError.MARKET_RESOURCE_TYPE_NOT_SUPPORTED);
            }
            purchase.setForkedResourceId(forkedResourceId);
            MarketPurchaseEntity saved = marketPurchaseRepository.save(purchase);
            log.info("market fork finished purchaseId={} sourceResourceId={} forkedResourceId={}",
                    saved.getPurchaseId(), saved.getSourceResourceId(), forkedResourceId);
            return BeanUtil.copyProperties(saved, MarketPurchaseResponse.class);
        } catch (Exception e) {
            resourceService.softRemoveResources(List.of(forkedResourceId));
            log.warn("market fork compensated purchaseId={} forkedResourceId={}", purchase.getPurchaseId(), forkedResourceId, e);
            throw e;
        }
    }

    @Override
    public PageR<MarketListingResponse> listMyListings(String sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        Page<MarketListingEntity> entityPage = marketListingRepository.findBySellerId(sellerId, pageable);
        List<String> sourceResourceIds = entityPage.getContent().stream()
                .map(MarketListingEntity::getSourceResourceId)
                .toList();
        Map<String, ResourceInteractionInfoEntity> interactionMap = sourceResourceIds.isEmpty()
                ? Collections.emptyMap()
                : resourceInteractionInfoRepository.findByResourceIdIn(sourceResourceIds).stream()
                .collect(Collectors.toMap(ResourceInteractionInfoEntity::getResourceId, entity -> entity));
        PageR<MarketListingResponse> pageR = new PageR<>(entityPage.getTotalElements(), page, size);
        pageR.addAll(entityPage.getContent().stream()
                .map(entity -> {
                    MarketListingResponse response = BeanUtil.copyProperties(entity, MarketListingResponse.class);
                    Map<String, String> tagMap = new HashMap<>();
                    if (entity.getTagIds() != null && !entity.getTagIds().isEmpty()) {
                        tagRepository.findAllById(entity.getTagIds()).forEach(tag -> tagMap.put(tag.getTagId(), tag.getTagName()));
                    }
                    response.setCurrentTags(tagMap);
                    response.setResourceInteractionInfo(
                            interactionMap.getOrDefault(entity.getSourceResourceId(), new ResourceInteractionInfoEntity()));
                    try {
                        Long seller = Long.valueOf(entity.getSellerId());
                        UserDisplayBase sellerInfo = remoteUserService.getUserDisplayInfo(List.of(seller)).getData().get(seller);
                        response.setSellerInfo(sellerInfo);
                    } catch (Exception e) {
                        log.debug("market seller info degraded sellerId={}", entity.getSellerId(), e);
                    }
                    return response;
                })
                .toList());
        return pageR;
    }

    @Override
    public PageR<MarketPurchaseResponse> listMyPurchases(String buyerId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        Page<MarketPurchaseEntity> entityPage = marketPurchaseRepository.findByBuyerId(buyerId, pageable);
        PageR<MarketPurchaseResponse> pageR = new PageR<>(entityPage.getTotalElements(), page, size);
        pageR.addAll(entityPage.getContent().stream()
                .map(entity -> BeanUtil.copyProperties(entity, MarketPurchaseResponse.class))
                .toList());
        return pageR;
    }

}
