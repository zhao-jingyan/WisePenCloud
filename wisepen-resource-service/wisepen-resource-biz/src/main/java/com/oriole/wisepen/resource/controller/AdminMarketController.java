package com.oriole.wisepen.resource.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.BusinessType;
import com.oriole.wisepen.common.core.domain.enums.IdentityType;
import com.oriole.wisepen.common.log.annotation.Log;
import com.oriole.wisepen.common.security.annotation.CheckRole;
import com.oriole.wisepen.resource.domain.dto.req.MarketAuditOfferRequest;
import com.oriole.wisepen.resource.domain.dto.req.MarketOffShelfOfferRequest;
import com.oriole.wisepen.resource.service.IMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员 - 集市", description = "管理员查审核上架请求")
@RestController
@RequestMapping("/admin/market")
@RequiredArgsConstructor
@CheckRole(IdentityType.ADMIN)
public class AdminMarketController {

    private final IMarketService marketService;

    @Operation(
            summary = "审核上架",
            description = """
                    - 用途：集市管理员处理资源上架申请。
                    - 请求：resourceId 指定待审核资源；marketGroupId 指定集市群；status 只能为 PUBLISHED、REJECTED 或 BANNED；auditMessage 是审核说明。
                    - 约束：当前用户必须是平台管理员；上架记录必须处于 PENDING；目标小组必须是集市组；驳回或封禁时必须填写审核说明。
                    - 处理：只对该集市群下处于 PENDING 的 marketOffer 写入审核状态、审核说明、审核时间和审核人；审核通过后对应 Fork 权益可被购买；不创建订单。
                    - 失败：上架记录不存在 -> ResourceError.MARKET_OFFER_NOT_FOUND；驳回或封禁未填写审核说明 -> ResourceError.MARKET_AUDIT_MESSAGE_REQUIRED；目标小组不是集市组 -> ResourceError.MARKET_GROUP_REQUIRED。
                    - 响应：成功时返回空结果。
                    """
    )
    @Log(title = "审核上架", businessType = BusinessType.UPDATE)
    @PostMapping("/auditOffer")
    public R<Void> auditOffer(@Valid @RequestBody MarketAuditOfferRequest request) {
        marketService.auditOffer(request, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRoleMap());
        return R.ok();
    }

    @Operation(
            summary = "管理员下架资源",
            description = """
                    - 用途：集市管理员将已提交到集市的资源下架。
                    - 请求：resourceId 指定已上架资源；marketGroupId 指定集市群；purchaseTypes 可选，为空时下架该集市群下全部权益。
                    - 约束：目标小组必须是集市组；当前用户必须是该集市群 OWNER、ADMIN。
                    - 处理：将指定 marketOffer 状态置为 OFF_SHELF；不修改集市标签绑定，不删除已有订单。
                    - 失败：上架记录不存在 -> ResourceError.MARKET_OFFER_NOT_FOUND；目标小组不是集市组 -> ResourceError.MARKET_GROUP_REQUIRED；当前用户无权操作 -> ResourceError.RESOURCE_PERMISSION_DENIED。
                    - 响应：成功时返回空结果。
                    """
    )
    @Log(title = "管理员下架资源", businessType = BusinessType.UPDATE)
    @PostMapping("/offShelfOffer")
    public R<Void> offShelfOffer(@Valid @RequestBody MarketOffShelfOfferRequest request) {
        marketService.offShelfOffer(request, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRoleMap());
        return R.ok();
    }
}
