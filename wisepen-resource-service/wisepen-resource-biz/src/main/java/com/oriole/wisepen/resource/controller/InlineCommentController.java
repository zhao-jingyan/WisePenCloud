package com.oriole.wisepen.resource.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.BusinessType;
import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.common.log.annotation.Log;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.resource.domain.dto.ResourceCheckPermissionReqDTO;
import com.oriole.wisepen.resource.domain.dto.ResourceCheckPermissionResDTO;
import com.oriole.wisepen.resource.domain.dto.req.InlineCommentCreateRequest;
import com.oriole.wisepen.resource.domain.dto.req.InlineCommentThreadCreateRequest;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentResponse;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentThreadChangesResponse;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentThreadListResponse;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentThreadResponse;
import com.oriole.wisepen.resource.enums.ResourceAction;
import com.oriole.wisepen.resource.exception.ResourceError;
import com.oriole.wisepen.resource.service.IInlineCommentService;
import com.oriole.wisepen.resource.service.IResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.Map;

@Tag(name = "资源行内批注", description = "资源行内批注 Thread 创建、回复、按 ID 召回与增量同步")
@RestController
@RequestMapping("/resource/threads")
@RequiredArgsConstructor
@CheckLogin
@Validated
public class InlineCommentController {

    private final IInlineCommentService inlineCommentService;
    private final IResourceService resourceService;

    @Operation(
            summary = "创建行内批注 Thread",
            description = """
                    - 用途：基于不透明的 RelativePosition 锚点创建一条行内批注 Thread 和首条 InlineComment。
                    - 请求：resourceId、idempotencyKey、anchor.start、anchor.end、quoteText 和 content 必传；anchor 编码仅存储，不解析正文或 Y.Doc。
                    - 约束：当前用户必须拥有 ResourceAction.INLINE_COMMENT；同一资源下重复 idempotencyKey 返回首次创建的完整 Thread。
                    - 处理：Thread、Anchor 与首条 InlineComment 作为一份 Mongo document 原子写入，初始 revision 为 1。
                    - 失败：资源动作权限裁决未通过 -> ResourceError.RESOURCE_PERMISSION_DENIED。
                    - 响应：返回包含权威 ID、作者快照、revision 和时间戳的完整 InlineCommentThreadResponse。
                    """
    )
    @PostMapping
    @Log(title = "创建资源行内批注 Thread", businessType = BusinessType.INSERT)
    public R<InlineCommentThreadResponse> createInlineCommentThread(
            @Valid @RequestBody InlineCommentThreadCreateRequest request
    ) {
        String operatorUserId = SecurityContextHolder.getUserId().toString();
        assertResourceAction(request.getResourceId(), ResourceAction.INLINE_COMMENT);
        return R.ok(inlineCommentService.createInlineCommentThread(request, operatorUserId));
    }

    @Operation(
            summary = "查询资源行内批注 Thread 列表",
            description = """
                    - 用途：加载指定资源当前全部批注 Thread，并取得后续 changes 轮询的起始 cursor。
                    - 请求：resourceId 必传。
                    - 约束：当前用户必须拥有 ResourceAction.VIEW；own_only 展示规则由前端依据 Yjs 文档设置执行。
                    - 处理：返回完整 Thread；后端不读取或解析 Yjs 正文与可见性设置。
                    - 失败：资源查看权限裁决未通过 -> ResourceError.RESOURCE_PERMISSION_DENIED。
                    - 响应：返回 items 与基于 updatedAt、threadId 生成的不透明 cursor。
                    """
    )
    @GetMapping
    public R<InlineCommentThreadListResponse> listInlineCommentThreads(
            @NotBlank @RequestParam String resourceId
    ) {
        assertResourceAction(resourceId, ResourceAction.VIEW);
        return R.ok(inlineCommentService.listInlineCommentThreads(resourceId));
    }

    @Operation(
            summary = "按 ID 查询行内批注 Thread",
            description = """
                    - 用途：按 threadId 召回一份权威 Thread。
                    - 请求：threadId 必传。
                    - 约束：当前用户必须拥有该 Thread 所属资源的 ResourceAction.VIEW。
                    - 失败：Thread 不存在 -> ResourceError.INLINE_COMMENT_NOT_FOUND；资源查看权限裁决未通过 -> ResourceError.RESOURCE_PERMISSION_DENIED。
                    - 响应：返回完整 InlineCommentThreadResponse。
                    """
    )
    @GetMapping("/{threadId}")
    public R<InlineCommentThreadResponse> getInlineCommentThread(@NotBlank @PathVariable String threadId) {
        String resourceId = inlineCommentService.getInlineCommentThreadResourceId(threadId);
        assertResourceAction(resourceId, ResourceAction.VIEW);
        return R.ok(inlineCommentService.getInlineCommentThread(threadId));
    }

    @Operation(
            summary = "追加行内批注",
            description = """
                    - 用途：向已有 Thread 原子追加一条 InlineComment。
                    - 请求：threadId、idempotencyKey 和 content 必传。
                    - 约束：当前用户必须拥有 Thread 所属资源的 ResourceAction.INLINE_COMMENT；同一 Thread 内重复 idempotencyKey 返回首次写入的 InlineComment。
                    - 处理：使用 revision CAS 原子执行 $push items、$inc revision 与 updatedAt 更新，并在冲突时重读重试。
                    - 失败：Thread 不存在 -> ResourceError.INLINE_COMMENT_NOT_FOUND；资源动作权限裁决未通过 -> ResourceError.RESOURCE_PERMISSION_DENIED；并发冲突持续无法提交 -> ResourceError.INLINE_COMMENT_REVISION_CONFLICT。
                    - 响应：返回数据库写入后的权威 InlineComment，revision 等于提交后的 Thread revision。
                    """
    )
    @PostMapping("/{threadId}/comments")
    @Log(title = "追加资源行内批注", businessType = BusinessType.INSERT)
    public R<InlineCommentResponse> addInlineComment(
            @NotBlank @PathVariable String threadId,
            @Valid @RequestBody InlineCommentCreateRequest request
    ) {
        String operatorUserId = SecurityContextHolder.getUserId().toString();
        String resourceId = inlineCommentService.getInlineCommentThreadResourceId(threadId);
        assertResourceAction(resourceId, ResourceAction.INLINE_COMMENT);
        return R.ok(inlineCommentService.addInlineComment(threadId, request, operatorUserId));
    }

    @Operation(
            summary = "按 ID 查询行内批注",
            description = """
                    - 用途：在指定 Thread 内按 commentId 精确召回权威 InlineComment。
                    - 请求：threadId、commentId 必传。
                    - 约束：当前用户必须拥有该 Thread 所属资源的 ResourceAction.VIEW。
                    - 失败：Thread 或 InlineComment 不存在 -> ResourceError.INLINE_COMMENT_NOT_FOUND；资源查看权限裁决未通过 -> ResourceError.RESOURCE_PERMISSION_DENIED。
                    - 响应：返回 InlineCommentResponse。
                    """
    )
    @GetMapping("/{threadId}/comments/{commentId}")
    public R<InlineCommentResponse> getInlineComment(
            @NotBlank @PathVariable String threadId,
            @NotBlank @PathVariable String commentId
    ) {
        String resourceId = inlineCommentService.getInlineCommentThreadResourceId(threadId);
        assertResourceAction(resourceId, ResourceAction.VIEW);
        return R.ok(inlineCommentService.getInlineComment(threadId, commentId));
    }

    @Operation(
            summary = "查询行内批注 Thread 变化",
            description = """
                    - 用途：从 cursor 之后增量查询发生变化的 Thread。
                    - 请求：resourceId 必传，cursor 可选且必须是服务端返回的不透明值。
                    - 约束：当前用户必须拥有 ResourceAction.VIEW；own_only 展示规则由前端依据 Yjs 文档设置执行。
                    - 处理：按 (updatedAt, threadId) 排他推进，每次最多返回 200 条，仅携带 threadId 与 revision。
                    - 失败：资源查看权限裁决未通过 -> ResourceError.RESOURCE_PERMISSION_DENIED；cursor 非法 -> ResourceError.INLINE_COMMENT_CURSOR_INVALID。
                    - 响应：返回变化项和下一次查询使用的不透明 cursor。
                    """
    )
    @GetMapping("/changes")
    public R<InlineCommentThreadChangesResponse> getInlineCommentChanges(
            @NotBlank @RequestParam String resourceId,
            @RequestParam(required = false) String cursor
    ) {
        assertResourceAction(resourceId, ResourceAction.VIEW);
        return R.ok(inlineCommentService.getInlineCommentChanges(resourceId, cursor));
    }

    private void assertResourceAction(String resourceId, ResourceAction requiredAction) {
        Long operatorUserId = SecurityContextHolder.getUserId();
        Map<Long, GroupRoleType> groupRoles = SecurityContextHolder.getGroupRoleMap();
        ResourceCheckPermissionResDTO permission = resourceService.checkPermission(
                ResourceCheckPermissionReqDTO.builder()
                        .resourceId(resourceId)
                        .userId(operatorUserId)
                        .groupRoles(groupRoles)
                        .build()
        );
        if (permission == null
                || permission.getAllowedActions() == null
                || !permission.getAllowedActions().contains(requiredAction)) {
            throw new ServiceException(ResourceError.RESOURCE_PERMISSION_DENIED);
        }
    }
}
