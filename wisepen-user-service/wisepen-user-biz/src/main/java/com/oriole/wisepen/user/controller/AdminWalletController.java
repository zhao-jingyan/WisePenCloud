package com.oriole.wisepen.user.controller;

import cn.hutool.core.util.IdUtil;
import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.BusinessType;
import com.oriole.wisepen.common.core.domain.enums.IdentityType;
import com.oriole.wisepen.common.log.annotation.Log;
import com.oriole.wisepen.common.security.annotation.CheckRole;
import com.oriole.wisepen.user.api.domain.dto.req.WalletChangeCoinBalanceRequest;
import com.oriole.wisepen.user.service.IWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员 - 钱包", description = "管理员调整用户钱包金币余额")
@RestController
@RequestMapping("/admin/user/wallet")
@RequiredArgsConstructor
@CheckRole(IdentityType.ADMIN)
public class AdminWalletController {

    private final IWalletService walletService;

    @Operation(
            summary = "调整用户金币余额",
            description = """
                    - 用途：管理员直接调整指定用户的钱包金币余额。
                    - 请求：userId 指定目标用户；changedCoin 为调整数量，正数表示增加、负数表示扣减；walletTransactionType 和 meta 记录本次调整的流水类型和业务备注。
                    - 约束：当前操作者必须具备管理员身份；扣减金币时目标用户余额必须足够。
                    - 处理：按调整数量更新用户金币余额，生成独立 traceId，并写入管理员操作流水；不修改信息点余额。
                    - 失败：当前操作者不是管理员 -> PermissionError.UNAUTHORIZED；金币余额不足 -> UserError.WALLET_COIN_INSUFFICIENT；金币变动失败 -> UserError.WALLET_COIN_CHANGE_FAILED。
                    - 响应：成功时返回空结果。
                    """
    )
    @PostMapping("/changeBalance")
    @Log(title = "管理员调整用户信息点", businessType = BusinessType.UPDATE)
    public R<Void> changeBalance(@RequestBody @Valid WalletChangeCoinBalanceRequest req) {
        Long operatorId = SecurityContextHolder.getUserId();
        String traceId = IdUtil.randomUUID();
        walletService.changeCoinBalance(req.getUserId(), operatorId, traceId, req.getChangedCoin(), req.getWalletTransactionType(), req.getMeta());
        return R.ok();
    }
}
