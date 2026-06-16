package com.oriole.wisepen.resource.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.BusinessType;
import com.oriole.wisepen.common.log.annotation.Log;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.resource.domain.dto.req.MarketPublishOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketOffShelfOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketPurchaseRequest;
import com.oriole.wisepen.resource.domain.dto.res.MarketOrderResponse;
import com.oriole.wisepen.resource.service.IMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "集市", description = "资源上架、购买和复制")
@RestController
@RequestMapping("/resource/market")
@RequiredArgsConstructor
@CheckLogin
@Validated
public class MarketController {

    private final IMarketService marketService;

    @Operation(
            summary = "提交上架信息",
            description = """
                    - 用途：资源所有者提交或修改资源集市上架信息，进入待审核状态。
                    - 请求：resourceId 指定资源；marketGroupId 指定集市群；tagIds 指定集市标签；forkOncePrice 是 Fork 一次价格，forkUnlimitedPrice 是 Fork 无限次价格，二者至少填写一个；offerVersion 是上架版本。
                    - 约束：当前用户必须是资源所有者；目标小组必须是集市组；已封禁的权益不可修改或重新提交。
                    - 处理：在该集市群的 groupBind 下按已填写价格创建或复用 marketOffer，将本次选择的权益状态置为 PENDING，更新集市标签绑定并触发资源 ACL 重算；未选择的权益不变；不创建订单。
                    - 失败：资源不存在 -> ResourceError.RESOURCE_NOT_FOUND；当前用户不是资源所有者 -> ResourceError.RESOURCE_PERMISSION_DENIED；目标小组不是集市组 -> ResourceError.MARKET_GROUP_REQUIRED；上架记录已封禁 -> ResourceError.MARKET_OFFER_BANNED。
                    - 响应：成功时返回空结果。
                    """
    )
    @Log(title = "提交上架信息", businessType = BusinessType.INSERT)
    @PostMapping("/publishOffer")
    public R<Void> publishOffer(@Valid @RequestBody MarketPublishOfferRequest request) {
        marketService.publishOffer(request, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRoleMap());
        return R.ok();
    }

    @Operation(
            summary = "下架资源",
            description = """
                    - 用途：卖家或集市管理员将已提交到集市的资源下架。
                    - 请求：resourceId 指定已上架资源；marketGroupId 指定集市群；purchaseTypes 可选，为空时下架该集市群下全部权益。
                    - 约束：目标小组必须是集市组；当前用户必须是卖家本人或该集市群 OWNER、ADMIN。
                    - 处理：将指定 marketOffer 状态置为 OFF_SHELF；不修改集市标签绑定，不删除已有订单。
                    - 失败：上架记录不存在 -> ResourceError.MARKET_OFFER_NOT_FOUND；目标小组不是集市组 -> ResourceError.MARKET_GROUP_REQUIRED；当前用户无权操作 -> ResourceError.RESOURCE_PERMISSION_DENIED。
                    - 响应：成功时返回空结果。
                    """
    )
    @Log(title = "下架资源", businessType = BusinessType.UPDATE)
    @PostMapping("/offShelfOffer")
    public R<Void> offShelfOffer(@Valid @RequestBody MarketOffShelfOfferRequest request) {
        marketService.offShelfOffer(request, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRoleMap());
        return R.ok();
    }

    @Operation(
            summary = "购买资源",
            description = """
                    - 用途：买家购买集市资源的 Fork 权益。
                    - 请求：resourceId 指定已上架资源；marketGroupId 指定集市群；purchaseType 指定 FORK_ONCE 或 FORK_UNLIMITED。
                    - 约束：上架记录必须处于 PUBLISHED；目标小组必须是集市组且资源仍绑定在该集市群；买家不能购买自己上架的资源。
                    - 处理：按 purchaseType 选中对应 marketOffer 并按其 price 结算，按 resourceId、marketGroupId、purchaseType、buyerId 做幂等；创建订单并记录集市群、购买权益、购买版本和 Fork 次数，随后调用复制流程执行首次 Fork；不修改原资源权限。
                    - 失败：上架记录不存在 -> ResourceError.MARKET_OFFER_NOT_FOUND；资源未上架或已下架 -> ResourceError.MARKET_OFFER_NOT_ACTIVE；不能购买自己上架的资源 -> ResourceError.MARKET_SELF_ORDER_NOT_ALLOWED；目标小组不是集市组 -> ResourceError.MARKET_GROUP_REQUIRED；资源未绑定该集市群 -> ResourceError.RESOURCE_PERMISSION_DENIED；购买权益类型无效 -> ResourceError.MARKET_PURCHASE_TYPE_INVALID。
                    - 响应：返回购买记录信息。
                    """
    )
    @Log(title = "购买资源", businessType = BusinessType.INSERT)
    @PostMapping("/purchase")
    public R<MarketOrderResponse> purchase(@Valid @RequestBody MarketPurchaseRequest request) {
        return R.ok(marketService.purchase(request, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRoleMap()));
    }

    @Operation(
            summary = "复制已购买资源",
            description = """
                    - 用途：买家基于已购买订单复制当前已上架版本到个人空间。
                    - 请求：路径参数 orderId 指定购买记录。
                    - 约束：购买记录必须属于当前用户；对应上架记录必须仍处于 PUBLISHED；FORK_ONCE 订单只能在 forkCount 小于 1 时执行。
                    - 处理：先读取订单并更新 forkCount，再按订单的 marketGroupId 与 purchaseType 定位当前 marketOffer，发布资源复制消息并复制其 offerVersion；复制消息最终进入 DLQ 时由资源服务补偿 forkCount。
                    - 失败：购买记录不存在 -> ResourceError.MARKET_ORDER_NOT_FOUND；当前用户无权操作 -> ResourceError.RESOURCE_PERMISSION_DENIED；购买权益类型无效 -> ResourceError.MARKET_PURCHASE_TYPE_INVALID；可用 Fork 次数已用完 -> ResourceError.MARKET_FORK_QUOTA_EXHAUSTED；资源不存在 -> ResourceError.RESOURCE_NOT_FOUND；资源未上架或已下架 -> ResourceError.MARKET_OFFER_NOT_ACTIVE。
                    - 响应：成功时返回空结果。
                    """
    )
    @Log(title = "复制已购买资源", businessType = BusinessType.INSERT)
    @PostMapping("/fork/{orderId}")
    public R<Void> fork(@PathVariable("orderId") @NotBlank String orderId) {
        marketService.fork(orderId, SecurityContextHolder.getUserId());
        return R.ok();
    }

    @Operation(
            summary = "分页查询我的购买",
            description = """
                    - 用途：查询当前用户购买过的集市资源订单，供用户查看购买类型并从订单入口继续 Fork。
                    - 请求：page、size 控制分页。
                    - 约束：当前用户必须已登录。
                    - 处理：按当前用户 buyerId 分页查询订单，返回购买类型、支付价格、购买版本和 Fork 次数；不校验原资源当前是否仍可 Fork。
                    - 失败：无业务失败分支。
                    - 响应：返回当前用户购买记录分页。
                    """
    )
    @GetMapping("/listMyOrders")
    public R<PageR<MarketOrderResponse>> listMyOrders(
            @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) int size) {
        return R.ok(marketService.listMyOrders(SecurityContextHolder.getUserId().toString(), page, size));
    }
}
