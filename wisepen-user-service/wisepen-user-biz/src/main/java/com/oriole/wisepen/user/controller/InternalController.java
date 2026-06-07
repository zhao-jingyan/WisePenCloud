package com.oriole.wisepen.user.controller;

import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.user.api.domain.base.GroupDisplayBase;
import com.oriole.wisepen.user.api.domain.base.UserDisplayBase;
import com.oriole.wisepen.user.api.domain.dto.req.WalletSettleCoinTradeRequest;
import com.oriole.wisepen.user.api.feign.RemoteUserService;
import com.oriole.wisepen.user.api.feign.RemoteWalletService;
import com.oriole.wisepen.user.service.IGroupService;
import com.oriole.wisepen.user.service.IUserService;
import com.oriole.wisepen.user.service.IWalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Tag(name = "内部 - 用户", description = "供其他微服务查询用户与小组展示信息，并结算钱包交易")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController implements RemoteUserService, RemoteWalletService {

    private final IUserService userService;
    private final IGroupService groupService;
    private final IWalletService walletService;

    @Override
    @Operation(
            summary = "内部获取用户展示信息",
            description = """
                    - 用途：供其他微服务批量获取用户展示字段。
                    - 请求：userIds 指定待查询用户 ID 列表。
                    - 约束：调用方必须来自可信服务链路；userIds 为空时返回空映射。
                    - 处理：批量读取用户账号展示字段并按 userId 组装映射；不返回密码、钱包、资料扩展或认证状态详情。
                    - 失败：用户列表解析或查询发生未处理异常 -> CommonError.INTERNAL_ERROR。
                    - 响应：返回以用户 ID 为键的展示信息映射，不存在的用户不会出现在结果中。
                    """
    )
    @GetMapping("/user/getUserDisplayInfo")
    public R<Map<Long, UserDisplayBase>> getUserDisplayInfo(List<Long> userIds) {
        return R.ok(userService.getUserDisplayInfoByIds(new HashSet<>(userIds)));
    }

    @Override
    @Operation(
            summary = "内部获取小组展示信息",
            description = """
                    - 用途：供其他微服务批量获取小组展示字段。
                    - 请求：groupIds 指定待查询小组 ID 列表。
                    - 约束：调用方必须来自可信服务链路；groupIds 为空时返回空映射。
                    - 处理：批量读取小组展示字段并按 groupId 组装映射；不返回成员列表、钱包额度或资源配置。
                    - 失败：小组列表解析或查询发生未处理异常 -> CommonError.INTERNAL_ERROR。
                    - 响应：返回以小组 ID 为键的展示信息映射，不存在的小组不会出现在结果中。
                    """
    )
    @GetMapping("/group/getGroupDisplayInfo")
    public R<Map<Long, GroupDisplayBase>> getGroupDisplayInfo(List<Long> groupIds) {
        return R.ok(groupService.getGroupDisplayInfoByIds(new HashSet<>(groupIds)));
    }


    @Override
    @Operation(
            summary = "内部结算金币交易",
            description = """
                    - 用途：供资源交易等业务服务完成买方到卖方的金币结算。
                    - 请求：traceId 用于幂等识别交易；buyerId、sellerId 和 price 描述买方、卖方和交易金额；meta 记录业务来源信息。
                    - 约束：调用方必须来自可信服务链路；买方金币余额必须足够；traceId 已结算时接口幂等返回成功。
                    - 处理：先扣减买方金币余额，再增加卖方金币余额，并分别写入钱包交易流水；不处理资源交付或订单状态。
                    - 失败：买方金币余额不足 -> UserError.WALLET_COIN_INSUFFICIENT；金币钱包变动失败 -> UserError.WALLET_COIN_CHANGE_FAILED。
                    - 响应：成功时返回空结果。
                    """
    )
    @PostMapping("/user/wallet/settleTrade")
    public R<Void> settleCoinTrade(@RequestBody @Valid WalletSettleCoinTradeRequest req) {
        walletService.settleCoinTrade(req);
        return R.ok();
    }
}
