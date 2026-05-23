package com.oriole.wisepen.user.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.enums.BusinessType;
import com.oriole.wisepen.common.log.annotation.Log;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.user.api.domain.dto.req.UserInfoUpdateRequest;
import com.oriole.wisepen.user.api.domain.dto.res.UserDetailInfoResponse;
import com.oriole.wisepen.user.api.domain.dto.VerificationResultDTO;
import com.oriole.wisepen.user.api.domain.dto.req.UserProfileUpdateRequest;
import com.oriole.wisepen.user.api.enums.UserVerificationMode;
import com.oriole.wisepen.user.service.IUserService;
import com.oriole.wisepen.user.strategy.VerificationStrategyFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "用户服务", description = "用户信息与认证状态")
public class UserController {

    private final IUserService userService;
    private final VerificationStrategyFactory verificationStrategyFactory;

    @CheckLogin
    @GetMapping("/getUserInfo")
    @Log(title = "用户信息获取", businessType= BusinessType.SELECT, isSaveResponseData=false)
    @Operation(summary = "获取当前用户信息", operationId = "getUserInfo")
    public R<UserDetailInfoResponse> getUserInfo() {
        Long userId = SecurityContextHolder.getUserId();
        UserDetailInfoResponse userInfo = userService.getUserInfoById(userId);
        return R.ok(userInfo);
    }

    @CheckLogin
    @PutMapping("/changeUserProfile")
    @Log(title = "更新用户资料", businessType = BusinessType.UPDATE)
    @Operation(summary = "更新用户资料", operationId = "changeUserProfile")
    public R<Void> updateUserProfile(@RequestBody UserProfileUpdateRequest dto) {
        userService.updateProfile(SecurityContextHolder.getUserId(), dto);
        return R.ok();
    }

    @CheckLogin
    @PutMapping("/changeUserInfo")
    @Log(title = "更新用户信息", businessType = BusinessType.UPDATE)
    @Operation(summary = "更新用户信息", operationId = "changeUserInfo")
    public R<Void> updateUserInfo(@RequestBody UserInfoUpdateRequest dto) {
        userService.updateUserInfo(SecurityContextHolder.getUserId(), dto);
        return R.ok();
    }

    @CheckLogin
    @PostMapping("/verify/initiateEmailVerify")
    @Log(title = "发起邮箱验证", businessType = BusinessType.OTHER)
    @Operation(summary = "发起邮箱验证", operationId = "initiateEmailVerify")
    public R<Void> initiateEmailVerify(@Parameter(description = "邮箱地址") @RequestParam("email") String email) {
        Map<String,Object> map = new HashMap<>();
        map.put("email", email);
        verificationStrategyFactory.getStrategy(UserVerificationMode.EDU_EMAIL)
                .initiate(SecurityContextHolder.getUserId(), map);
        return R.ok();
    }

    @GetMapping("/verify/checkEmailVerify")
    @Log(title = "邮箱验证回调", businessType = BusinessType.OTHER)
    @Operation(summary = "检查邮箱验证", operationId = "checkEmailVerify")
    public R<Void> checkEmailVerify(@Parameter(description = "邮箱验证 token") @RequestParam("token") String token) {
        Map<String,Object> map = new HashMap<>();
        map.put("token", token);
        verificationStrategyFactory.getStrategy(UserVerificationMode.EDU_EMAIL).verify(map);
        return R.ok();
    }

    @CheckLogin
    @PostMapping("/verify/initiateFudanUISVerify")
    @Log(title = "发起复旦UIS认证", businessType = BusinessType.OTHER)
    @Operation(summary = "发起复旦 UIS 认证", operationId = "initiateFudanUISVerify")
    public R<Void> initiateFudanUISVerify(
            @Parameter(description = "UIS 账号") @RequestParam("uisAccount") String uisAccount,
            @Parameter(description = "UIS 密码") @RequestParam("uisPassword") String uisPassword) {
        Map<String,Object> map = new HashMap<>();
        map.put("uisAccount", uisAccount);
        map.put("uisPassword", uisPassword);
        verificationStrategyFactory.getStrategy(UserVerificationMode.FDU_UIS_SYS)
                .initiate(SecurityContextHolder.getUserId(), map);
        return R.ok();
    }

    @CheckLogin
    @GetMapping("/verify/checkFudanUISVerify")
    @Log(title = "检查复旦UIS认证状态", businessType = BusinessType.OTHER)
    @Operation(summary = "检查复旦 UIS 认证状态", operationId = "checkFudanUISVerify")
    public R<VerificationResultDTO> checkFudanUISVerify() {
        Map<String,Object> map = new HashMap<>();
        map.put("userId", SecurityContextHolder.getUserId());
        VerificationResultDTO dto = verificationStrategyFactory.getStrategy(UserVerificationMode.FDU_UIS_SYS).verify(map);
        return R.ok(dto);
    }
}
