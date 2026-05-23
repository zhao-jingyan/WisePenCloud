package com.oriole.wisepen.user.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.user.api.domain.dto.req.WalletRedeemVoucherRequest;
import com.oriole.wisepen.user.api.domain.dto.req.WalletTransferTokenRequest;
import com.oriole.wisepen.user.api.domain.dto.res.WalletDetailResponse;
import com.oriole.wisepen.user.api.domain.dto.res.WalletTransactionRecordResponse;
import com.oriole.wisepen.user.api.enums.TokenPayerType;
import com.oriole.wisepen.user.api.enums.TokenTransactionType;
import com.oriole.wisepen.user.service.IWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/wallet/")
@RequiredArgsConstructor
@Validated
@CheckLogin
@Tag(name = "钱包服务", description = "用户和小组 token 钱包与交易流水")
public class WalletController {

    private final IWalletService walletService;

    @GetMapping("/getUserWalletInfo")
    @Operation(summary = "获取当前用户钱包信息", operationId = "getUserWalletInfo")
    public R<WalletDetailResponse> getUserWalletInfo() {
        Long userId = SecurityContextHolder.getUserId();
        return R.ok(walletService.getUserWalletInfo(userId));
    }

    @PostMapping("/redeemVoucher")
    @Operation(summary = "兑换 token 礼包码", operationId = "redeemVoucher")
    public R<Void> redeemVoucher(@RequestBody WalletRedeemVoucherRequest req) {
        Long userId = SecurityContextHolder.getUserId();
        walletService.redeemVoucher(userId, req.getVoucherCode());
        return R.ok();
    }

    @PostMapping("/transferTokenBetweenGroupAndUser")
    @Operation(summary = "用户与小组之间转移 token", operationId = "transferTokenBetweenGroupAndUser")
    public R<Void> transferTokenBetweenGroupAndUser(@RequestBody @Valid WalletTransferTokenRequest req) {
        Long userId = SecurityContextHolder.getUserId();
        SecurityContextHolder.assertGroupRole(req.getGroupId(), GroupRoleType.OWNER);
        walletService.transferTokenBetweenGroupAndUser(userId, req);
        return R.ok();
    }

    @GetMapping("/listTransactions")
    @Operation(summary = "分页查询 token 交易流水", operationId = "listWalletTransactions")
    public R<PageR<WalletTransactionRecordResponse>> listTransactions(
            @Parameter(description = "小组 ID，不传则查询当前用户钱包") @RequestParam(value = "groupId", required = false) Long groupId,
            @Parameter(description = "交易类型") @RequestParam(value = "type", required = false) TokenTransactionType tokenTransactionType,
            @Parameter(description = "页码，从 1 开始") @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
            @Parameter(description = "每页数量") @RequestParam(value = "size", defaultValue = "20") @Min(1) Integer size
            ) {
        TokenPayerType payerType;
        Long payerId;
        if (groupId == null) {
            payerType = TokenPayerType.USER;
            payerId = SecurityContextHolder.getUserId();
        } else {
            payerType = TokenPayerType.GROUP;
            payerId = groupId;
            SecurityContextHolder.assertGroupRole(groupId, GroupRoleType.OWNER);
        }
        return R.ok(walletService.listTransactions(payerType, payerId, tokenTransactionType, page, size));
    }
}
