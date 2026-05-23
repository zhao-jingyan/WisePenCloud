package com.oriole.wisepen.user.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.common.core.domain.enums.GroupType;
import com.oriole.wisepen.common.core.domain.enums.IdentityType;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.common.security.exception.PermissionError;
import com.oriole.wisepen.common.security.exception.PermissionException;
import com.oriole.wisepen.user.api.domain.dto.req.GroupCreateRequest;
import com.oriole.wisepen.user.api.domain.dto.req.GroupDeleteRequest;
import com.oriole.wisepen.user.api.domain.dto.req.GroupMemberJoinRequest;
import com.oriole.wisepen.user.api.domain.dto.req.GroupUpdateRequest;
import com.oriole.wisepen.user.api.domain.dto.res.GroupDetailInfoResponse;
import com.oriole.wisepen.user.api.domain.dto.res.GroupItemInfoResponse;
import com.oriole.wisepen.user.api.enums.GroupRoleFilter;
import com.oriole.wisepen.user.service.IGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/group")
@RequiredArgsConstructor
@Validated
@CheckLogin
@Tag(name = "小组管理", description = "小组创建、更新、删除、列表与详情")
public class GroupController {

	private final IGroupService groupService;

	@PostMapping("/joinGroup")
	@Operation(summary = "加入小组", operationId = "joinGroup")
	public R<Void> joinGroup(@RequestBody @Valid GroupMemberJoinRequest req) {
		groupService.joinGroup(req, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRoleMap().keySet());
		// SecurityContextHolder.getGroupRoleMap().keySet()防止用户重复加群
		return R.ok();
	}

	@PostMapping("/addGroup")
	@Operation(summary = "创建小组", operationId = "createGroup")
	public R<Long> createGroup(@RequestBody @Valid GroupCreateRequest req) {

		IdentityType userIdentityType= SecurityContextHolder.getIdentityType();
		if (req.getGroupType() == GroupType.ADVANCED_GROUP && userIdentityType == IdentityType.STUDENT) {
			throw new PermissionException(PermissionError.UNAUTHORIZED);
		}

		if (req.getGroupType()==GroupType.MARKET_GROUP && userIdentityType != IdentityType.ADMIN) {
			throw new PermissionException(PermissionError.UNAUTHORIZED);
		}

		return R.ok(groupService.createGroup(req, SecurityContextHolder.getUserId()));
	}

	@PostMapping("/changeGroup")
	@Operation(summary = "更新小组信息", operationId = "changeGroup")
	public R<Void> updateGroup(@RequestBody @Valid GroupUpdateRequest req) {
		SecurityContextHolder.assertGroupRole(req.getGroupId(), GroupRoleType.OWNER);

		IdentityType userIdentityType= SecurityContextHolder.getIdentityType();
		if (req.getGroupType() == GroupType.ADVANCED_GROUP && userIdentityType == IdentityType.STUDENT) {
			throw new PermissionException(PermissionError.UNAUTHORIZED);
		}

		if (req.getGroupType()==GroupType.MARKET_GROUP && userIdentityType != IdentityType.ADMIN) {
			throw new PermissionException(PermissionError.UNAUTHORIZED);
		}
		groupService.updateGroup(req);
		return R.ok();
	}

	@PostMapping("/removeGroup")
	@Operation(summary = "删除小组", operationId = "removeGroup")
	public R<Void> deleteGroup(@RequestBody @Valid GroupDeleteRequest req) {
		SecurityContextHolder.assertGroupRole(req.getGroupId(), GroupRoleType.OWNER);
		Long userId=SecurityContextHolder.getUserId();
		groupService.deleteGroup(userId, req);
		return R.ok();
	}

	@GetMapping("/list")
	@Operation(summary = "分页查询小组", operationId = "listGroups")
	public R<PageR<GroupItemInfoResponse>> listGroups(
			@Parameter(description = "小组关系筛选") @RequestParam GroupRoleFilter groupRoleFilter,
			@Parameter(description = "页码，从 1 开始") @RequestParam(value = "page", defaultValue = "1") int page,
			@Parameter(description = "每页数量") @RequestParam(value = "size", defaultValue = "20") int size
	) {
		return R.ok(groupService.getGroupList(SecurityContextHolder.getUserId(), groupRoleFilter, page, size));
	}

	@GetMapping("/getGroupBaseInfo")
	@Operation(summary = "获取小组基础信息", operationId = "getGroupBaseInfo")
	public R<GroupItemInfoResponse> getGroupBaseInfo(@Parameter(description = "小组 ID") @RequestParam("groupId") Long groupId) {
		return R.ok(groupService.getGroupBaseInfoById(groupId));
	}

	@GetMapping("/getGroupDetailInfo")
	@Operation(summary = "获取小组详情", operationId = "getGroupDetailInfo")
	public R<GroupDetailInfoResponse> getGroupDetailInfo(@Parameter(description = "小组 ID") @RequestParam("groupId") Long groupId) {
		SecurityContextHolder.assertGroupRole(groupId, GroupRoleType.OWNER, GroupRoleType.ADMIN);
		return R.ok(groupService.getGroupDetailInfoById(groupId));
	}
}
