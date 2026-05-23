package com.oriole.wisepen.user.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.user.api.domain.dto.req.GroupMemberKickRequest;
import com.oriole.wisepen.user.api.domain.dto.req.GroupMemberRoleUpdateRequest;
import com.oriole.wisepen.user.api.domain.dto.req.GroupMemberQuitRequest;
import com.oriole.wisepen.user.api.domain.dto.req.GroupMemberTokenLimitUpdateRequest;
import com.oriole.wisepen.user.api.domain.dto.res.GroupMemberDetailResponse;
import com.oriole.wisepen.user.api.domain.dto.res.GroupMemberTokenDetailResponse;
import com.oriole.wisepen.user.service.IGroupMemberService;
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
@RequestMapping("/group/member")
@RequiredArgsConstructor
@Validated
@CheckLogin
@Tag(name = "小组成员管理", description = "小组成员退出、移除、角色与额度管理")
public class GroupMemberController {

	private final IGroupMemberService groupMemberService;
	private final IWalletService walletService;

	@PostMapping("/quit")
	@Operation(summary = "退出小组", operationId = "quitGroup")
	public R<Void> quitGroup(@RequestBody @Valid GroupMemberQuitRequest req) {
		SecurityContextHolder.assertInGroup(req.getGroupId()); // 用户退群必须先在群中
		groupMemberService.quitGroup(req, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRole(req.getGroupId()));
		return R.ok();
	}

	@PostMapping("/kick")
	@Operation(summary = "移除小组成员", operationId = "kickGroupMembers")
	public R<Void> kickGroupMembers(@RequestBody @Valid GroupMemberKickRequest req) {
		SecurityContextHolder.assertGroupRole(req.getGroupId(), GroupRoleType.OWNER, GroupRoleType.ADMIN);
		groupMemberService.kickGroupMembers(req, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRole(req.getGroupId()));
		return R.ok();
	}

	@PostMapping("/changeRole")
	@Operation(summary = "修改小组成员角色", operationId = "changeGroupMemberRole")
	public R<Void> changeRole(@RequestBody @Valid GroupMemberRoleUpdateRequest req) {
		SecurityContextHolder.assertGroupRole(req.getGroupId(), GroupRoleType.OWNER); //必须是所有者才能修改成员权限
		groupMemberService.updateGroupMemberRole(req, SecurityContextHolder.getUserId());
		return R.ok();
	}

	@GetMapping("/list")
	@Operation(summary = "分页查询小组成员", operationId = "listGroupMembers")
	public R<PageR<GroupMemberDetailResponse>> listGroupMembers(
			@Parameter(description = "小组 ID") @RequestParam("groupId") Long groupId,
			@Parameter(description = "页码，从 1 开始") @RequestParam(value = "page", defaultValue = "1") @Min(1) int page,
			@Parameter(description = "每页数量") @RequestParam(value = "size", defaultValue = "20") @Min(1) int size
	) {
		SecurityContextHolder.assertInGroup(groupId);
		return R.ok(groupMemberService.getGroupMemberList(groupId, page, size));
	}

	@GetMapping("/getMyGroupMemberInfo")
	@Operation(summary = "获取我的小组成员信息", operationId = "getMyGroupMemberInfo")
	public R<GroupMemberDetailResponse> getMyGroupMemberInfo(@Parameter(description = "小组 ID") @RequestParam("groupId") Long groupId) {
		return R.ok(groupMemberService.getGroupMemberInfoByUserId(groupId, SecurityContextHolder.getUserId()));
	}

	@GetMapping("/getMyRole")
	@Operation(summary = "获取我在小组中的角色", operationId = "getMyGroupRole")
	public R<GroupRoleType> getMyRole(@Parameter(description = "小组 ID") @RequestParam("groupId") Long groupId) {
		return R.ok(SecurityContextHolder.getGroupRole(groupId));
	}

	@PostMapping("/changeTokenLimit")
	@Operation(summary = "修改小组成员 token 限额", operationId = "changeGroupMemberTokenLimit")
	public R<Void> changeTokenLimit(@RequestBody @Valid GroupMemberTokenLimitUpdateRequest req) {
		SecurityContextHolder.assertGroupRole(req.getGroupId(), GroupRoleType.OWNER);
		walletService.updateGroupMemberTokenLimit(req);
		return R.ok();
	}

	@GetMapping("/getAllMyGroupTokenInfo")
	@Operation(summary = "查询我在所有小组中的 token 信息", operationId = "getAllMyGroupTokenInfo")
	public R<PageR<GroupMemberTokenDetailResponse>> getAllMyGroupTokenInfo(
			@Parameter(description = "页码，从 1 开始") @RequestParam(value = "page", defaultValue = "1") @Min(1) Integer page,
			@Parameter(description = "每页数量") @RequestParam(value = "size", defaultValue = "20") @Min(1) Integer size
	){
		Long userId = SecurityContextHolder.getUserId();
		return R.ok(walletService.getAllGroupTokenInfoByUserId(userId, page, size));
	}
}
