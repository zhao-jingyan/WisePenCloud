package com.oriole.wisepen.resource.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.enums.GroupType;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.resource.domain.GroupTagBind;
import com.oriole.wisepen.resource.domain.MarketOfferOption;
import com.oriole.wisepen.resource.domain.base.MarketOfferInfoBase;
import com.oriole.wisepen.resource.domain.dto.req.MarketAuditOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketOffShelfOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketPublishOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketPurchaseRequest;
import com.oriole.wisepen.resource.domain.dto.res.MarketOrderResponse;
import com.oriole.wisepen.resource.domain.entity.MarketOrderEntity;
import com.oriole.wisepen.resource.domain.entity.ResourceItemEntity;
import com.oriole.wisepen.resource.enums.MarketOfferStatus;
import com.oriole.wisepen.resource.enums.ResourceAction;
import com.oriole.wisepen.resource.exception.ResourceError;
import com.oriole.wisepen.resource.mq.IResourceEventPublisher;
import com.oriole.wisepen.resource.repository.MarketOrderRepository;
import com.oriole.wisepen.resource.repository.ResourceItemRepository;
import com.oriole.wisepen.resource.repository.TagRepository;
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
import java.util.*;

import static com.oriole.wisepen.resource.constant.ResourceConstants.MARKET_GROUP_PREFIX;
import static com.oriole.wisepen.resource.enums.ResourceAction.MARKET_FORBIDDEN_ACTIONS_MASK;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketServiceImpl implements IMarketService {

    private final IResourceService resourceService;

    private final MarketOrderRepository marketOrderRepository;
    private final ResourceItemRepository resourceItemRepository;
    private final TagRepository tagRepository;
    private final IResourceEventPublisher resourceEventPublisher;
    private final RemoteUserService remoteUserService;
    private final RemoteWalletService remoteWalletService;


    @Override
    // 上架
    public void publishOffer(MarketPublishOfferRequest request) {
        ResourceItemEntity resource = resourceItemRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ServiceException(ResourceError.RESOURCE_NOT_FOUND));

        String marketGroupId = request.getMarketGroupId();
        // 检查 groupID对应的小组是否是 MARKET_GROUP
        Map<Long, GroupDisplayBase> groupMap = remoteUserService.getGroupDisplayInfo(List.of(Long.valueOf(marketGroupId))).getData();
        GroupDisplayBase groupInfo = groupMap == null ? null : groupMap.get(Long.valueOf(marketGroupId));
        if (groupInfo == null || groupInfo.getGroupType() != GroupType.MARKET_GROUP) {
            throw new ServiceException(ResourceError.MARKET_GROUP_REQUIRED);
        }

        // MARKET 组的 Tag 在 MARKET_GROUP_PREFIX 前缀的 groupId 下
        resourceService.findAndValidateTags(MARKET_GROUP_PREFIX + marketGroupId, request.getTagIds());

        List<GroupTagBind> groupBinds = resource.getGroupBinds() == null ? new ArrayList<>() : resource.getGroupBinds();
        // 寻找该实体中是否已经存在当前 groupId 的绑定记录
        GroupTagBind groupBind = groupBinds.stream().filter(groupTagBind -> groupTagBind.getGroupId().equals(marketGroupId)).findFirst()
                .orElse(GroupTagBind.builder().groupId(marketGroupId).tagIds(request.getTagIds()).build());

        MarketOfferOption marketOfferOption = groupBind.getMarketOffer() == null ? new MarketOfferOption() : groupBind.getMarketOffer();

        if (marketOfferOption.getStatus() == MarketOfferStatus.BANNED) { // 被禁的资源不能上架
            throw new ServiceException(ResourceError.MARKET_OFFER_BANNED);
        }

        marketOfferOption.setReviewContentPercentage(request.getReviewContentPercentage());
        marketOfferOption.setReviewActionsMask(ResourceAction.actionsToPermissionCode(request.getReviewActions()) & ~MARKET_FORBIDDEN_ACTIONS_MASK);
        marketOfferOption.setMarketOfferList(request.getMarketOfferList().stream().map(
                (marketOfferInfo) -> {
                    int grantedActionsMask = ResourceAction.actionsToPermissionCode(marketOfferInfo.getGrantedActions()) & ~MARKET_FORBIDDEN_ACTIONS_MASK;
                    return MarketOfferInfoBase.builder().price(marketOfferInfo.getPrice()).grantedActionsMask(grantedActionsMask).createAt(LocalDateTime.now()).build();
                }).toList());

        if ((marketOfferOption.getStatus() == MarketOfferStatus.PUBLISHED || marketOfferOption.getStatus() == MarketOfferStatus.OFF_SHELF)
                && marketOfferOption.getOfferVersion().equals(request.getOfferVersion())) {
            marketOfferOption.setStatus(MarketOfferStatus.PUBLISHED); // 如果此前是已发布或下架状态，且没有改变上架的版本，则直接上架
        } else {
            // 否则需要审核
            marketOfferOption.setOfferVersion(request.getOfferVersion());
            marketOfferOption.setStatus(MarketOfferStatus.PENDING);
            // 在审核前，该资源不能从 MARKET 组获得任何权限
            if (resource.getOverrideGrantedActionsMask() == null) resource.setOverrideGrantedActionsMask(new HashMap<>());
            resource.getOverrideGrantedActionsMask().put(marketGroupId, 0);
        }
        groupBind.setMarketOffer(marketOfferOption);
        resource.setGroupBinds(groupBinds);
        resourceItemRepository.save(resource);

        if (marketOfferOption.getStatus() == MarketOfferStatus.PUBLISHED) { // 如果为发布状态，则触发 ACL 重算，否则不触发
            resourceEventPublisher.publishAclRecalculateEvent(resource.getResourceId(), "MARKET_OFFER_PUBLISHED");
        }

        log.info("market offer submitted. resourceId={} marketGroupId={} offerVersion={}",
                resource.getResourceId(), marketGroupId, request.getOfferVersion());
    }

    @Override
    public void offShelfOffer(MarketOffShelfOfferRequest request) {
        ResourceItemEntity resource = resourceItemRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ServiceException(ResourceError.RESOURCE_NOT_FOUND));
        String marketGroupId = request.getMarketGroupId();

        MarketOfferOption marketOfferOption = getMarketOfferOption(resource, marketGroupId);

        if (marketOfferOption.getStatus() == MarketOfferStatus.BANNED) { // 被禁的资源不能下架
            throw new ServiceException(ResourceError.MARKET_OFFER_BANNED);
        } else if(marketOfferOption.getStatus() != MarketOfferStatus.PUBLISHED) { // 未上架的资源不能下架
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_ACTIVE);
        }

        marketOfferOption.setStatus(MarketOfferStatus.OFF_SHELF);
        resource.getOverrideGrantedActionsMask().put(marketGroupId, 0); // 在上架前，该资源不能从 MARKET 组获得任何权限

        resourceItemRepository.save(resource);
        resourceEventPublisher.publishAclRecalculateEvent(resource.getResourceId(), "MARKET_OFFER_OFF_SHELF");
        log.info("market offer off-shelved. resourceId={} marketGroupId={}", resource.getResourceId(), marketGroupId);
    }

    @Override
    public void auditOffer(MarketAuditOfferRequest request, String operatorId) {
        // 当拒绝或封禁资源时，必须提供理由
        if ((request.getStatus() == MarketOfferStatus.REJECTED || request.getStatus() == MarketOfferStatus.BANNED)
                && !StringUtils.hasText(request.getAuditMessage())) {
            throw new ServiceException(ResourceError.MARKET_AUDIT_MESSAGE_REQUIRED);
        }

        ResourceItemEntity resource = resourceItemRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ServiceException(ResourceError.RESOURCE_NOT_FOUND));
        String marketGroupId = request.getMarketGroupId();

        MarketOfferOption marketOfferOption = getMarketOfferOption(resource, marketGroupId);

        MarketOfferStatus oldMarketOfferStatus = marketOfferOption.getStatus(); // 旧状态

        marketOfferOption.setStatus(request.getStatus());
        marketOfferOption.setAuditMessage(request.getAuditMessage());
        marketOfferOption.setAuditAt(LocalDateTime.now());
        marketOfferOption.setAuditorId(operatorId);

        if (request.getStatus() == MarketOfferStatus.PUBLISHED) {
            resource.getOverrideGrantedActionsMask().remove(marketGroupId); // 上架，该资源可从 MARKET 组获得权限
        } else {
            resource.getOverrideGrantedActionsMask().put(marketGroupId, 0); // 在上架前，该资源不能从 MARKET 组获得任何权限
        }
        resourceItemRepository.save(resource);

        if (marketOfferOption.getStatus() == MarketOfferStatus.PUBLISHED && oldMarketOfferStatus != marketOfferOption.getStatus()) {
            // 如果改变后为发布状态（说明是新上架），则触发 ACL 重算
            resourceEventPublisher.publishAclRecalculateEvent(resource.getResourceId(), "MARKET_OFFER_PUBLISHED");
        } else if(oldMarketOfferStatus == MarketOfferStatus.PUBLISHED && oldMarketOfferStatus != marketOfferOption.getStatus()) {
            // 如果改变前为发布状态（说明是新下架），则触发 ACL 重算
            resourceEventPublisher.publishAclRecalculateEvent(resource.getResourceId(), "MARKET_OFFER_OFF_SHELF");
        }

        log.info("market offer audited. resourceId={} operatorId={} marketGroupId={} status={} offerVersion={}",
                resource.getResourceId(), operatorId, marketGroupId, request.getStatus(), marketOfferOption.getOfferVersion());
    }

    @Override
    public MarketOrderResponse purchase(MarketPurchaseRequest request, String buyerId) {
        ResourceItemEntity resource = resourceItemRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ServiceException(ResourceError.RESOURCE_NOT_FOUND));
        String marketGroupId = request.getMarketGroupId();

        MarketOfferOption marketOfferOption = getMarketOfferOption(resource, marketGroupId);

        // 购买的资源必须是上架状态
        if (marketOfferOption.getStatus() != MarketOfferStatus.PUBLISHED) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_ACTIVE);
        }

        // 不能自己购买自己的资源
        if (buyerId.equals(resource.getOwnerId())) {
            throw new ServiceException(ResourceError.MARKET_SELF_ORDER_NOT_ALLOWED);
        }

        // 检查购买的 OfferID 是否存在
        MarketOfferInfoBase offer = marketOfferOption.getMarketOfferList().stream()
                .filter(item->item.getOfferId().equals(request.getOfferId()))
                .findFirst()
                .orElseThrow(() -> new ServiceException(ResourceError.MARKET_PURCHASE_TYPE_INVALID));

        // 不能重复购买，检查要购买的权限该用户是否本身就完全拥有

        List<ResourceAction> buyerHasActions = ResourceAction.permissionCodeToActions(marketOfferOption.getMarketSpecifiedUsersGrantedActionsMask().get(buyerId));
        List<ResourceAction> offerGrantedActions= ResourceAction.permissionCodeToActions(offer.getGrantedActionsMask());

        if (new HashSet<>(buyerHasActions).containsAll(offerGrantedActions)){
            throw new ServiceException(ResourceError.MARKET_ORDER_ALREADY_EXISTS);
        }

        String traceId = IdUtil.fastSimpleUUID();
        Integer paidPrice = offer.getPrice();
        String billMeta = "%s (%s)".formatted(resource.getResourceId(), offerGrantedActions.stream().map(Enum::name).toList().toString());

        // 请求交易
        remoteWalletService.settleCoinTrade(WalletSettleCoinTradeRequest.builder()
                .traceId(traceId)
                .buyerId(Long.valueOf(buyerId)).sellerId(Long.valueOf(resource.getOwnerId()))
                .price(paidPrice).meta(billMeta)
                .build());

        // 记录交易记录
        MarketOrderEntity order = MarketOrderEntity.builder()
                .traceId(traceId)
                .buyerId(buyerId).sellerId(resource.getOwnerId())
                .marketGroupId(marketGroupId)
                .purchasedResourceId(resource.getResourceId())
                .purchasedOfferVersion(marketOfferOption.getOfferVersion())
                .buyerGrantedActionsMask(offer.getGrantedActionsMask()).buyerPaidPrice(paidPrice).build();
        MarketOrderEntity saved = marketOrderRepository.save(order);

        // 添加权限
        Map<String, Integer> userMasks = marketOfferOption.getMarketSpecifiedUsersGrantedActionsMask() == null ? new HashMap<>() : marketOfferOption.getMarketSpecifiedUsersGrantedActionsMask();
        int existingMask = userMasks.getOrDefault(buyerId, 0);
        userMasks.put(buyerId, existingMask | offer.getGrantedActionsMask());
        marketOfferOption.setMarketSpecifiedUsersGrantedActionsMask(userMasks);

        resourceItemRepository.save(resource);
        resourceEventPublisher.publishAclRecalculateEvent(resource.getResourceId(), "MARKET_PURCHASE");

        log.info("market order created. orderId={} resourceId={} marketGroupId={} buyerId={} grantedActionsMask={} offerVersion={}",
                saved.getOrderId(), resource.getResourceId(), marketGroupId, buyerId, offer.getGrantedActionsMask() , marketOfferOption.getOfferVersion());
        return BeanUtil.copyProperties(saved, MarketOrderResponse.class);
    }

    @Override
    public PageR<MarketOrderResponse> listOrders(String buyerId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        Page<MarketOrderEntity> entityPage = marketOrderRepository.findByBuyerId(buyerId, pageable);
        PageR<MarketOrderResponse> pageR = new PageR<>(entityPage.getTotalElements(), page, size);

        pageR.addAll(entityPage.getContent().stream()
                .map(entity -> BeanUtil.copyProperties(entity, MarketOrderResponse.class))
                .toList());
        return pageR;
    }

    private MarketOfferOption getMarketOfferOption(ResourceItemEntity resource, String marketGroupId) {
        if (resource.getGroupBinds() == null) throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);

        GroupTagBind groupTagBind = resource.getGroupBinds().stream()
                .filter(bind -> marketGroupId.equals(bind.getGroupId()))
                .findFirst().orElseThrow(() -> new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND));

        MarketOfferOption marketOfferOption = groupTagBind.getMarketOffer();
        if (marketOfferOption == null || marketOfferOption.getMarketOfferList() == null || marketOfferOption.getMarketOfferList().isEmpty()) {
            throw new ServiceException(ResourceError.MARKET_OFFER_NOT_FOUND);
        }
        return marketOfferOption;
    }
}
