package com.oriole.wisepen.note.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.BusinessType;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.common.log.annotation.Log;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.note.api.domain.base.NoteInfoBase;
import com.oriole.wisepen.note.api.domain.dto.req.NoteCreateRequest;
import com.oriole.wisepen.note.api.domain.dto.res.NoteInfoResponse;
import com.oriole.wisepen.note.api.domain.dto.res.NoteVersionListResponse;
import com.oriole.wisepen.note.service.INoteService;
import com.oriole.wisepen.note.service.INoteVersionService;
import com.oriole.wisepen.resource.domain.dto.ResourceCheckPermissionReqDTO;
import com.oriole.wisepen.resource.domain.dto.ResourceCheckPermissionResDTO;
import com.oriole.wisepen.resource.domain.dto.ResourceInfoGetReqDTO;
import com.oriole.wisepen.resource.domain.dto.res.ResourceItemResponse;
import com.oriole.wisepen.resource.enums.ResourceAccessRole;
import com.oriole.wisepen.resource.feign.RemoteResourceService;
import com.oriole.wisepen.user.api.domain.base.UserDisplayBase;
import com.oriole.wisepen.user.api.feign.RemoteUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.oriole.wisepen.note.exception.NoteError.NOTE_PERMISSION_DENIED;

@Slf4j
@Tag(name = "笔记", description = "笔记创建、详情查询、版本列表和版本回退")
@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
@CheckLogin
public class NoteController {

    private final INoteService noteService;
    private final INoteVersionService noteVersionService;
    private final RemoteResourceService remoteResourceService;
    private final RemoteUserService remoteUserService;

    @Operation(
            summary = "创建笔记",
            description = """
                    - 用途：为当前用户创建一份新的协同笔记资源。
                    - 请求：title 为笔记标题。
                    - 约束：当前用户必须已登录；title 必须是可用于展示的笔记标题。
                    - 处理：调用资源服务注册 NOTE 类型资源，以当前用户作为所有者；随后创建笔记信息记录并将当前用户写入作者列表；不创建初始协同快照。
                    - 失败：未登录 -> PermissionError.NOT_LOGIN；资源注册失败或笔记信息落库失败 -> NoteError.NOTE_REGISTER_RESOURCE_FAILED。
                    - 响应：返回新笔记的资源 ID。
                    """
    )
    @Log(title = "创建笔记", businessType = BusinessType.INSERT)
    @PostMapping("/addNote")
    public R<String> createNote(@Validated @RequestBody NoteCreateRequest request) {
        String userId = SecurityContextHolder.getUserId().toString();
        String resourceId = noteService.createNote(request, userId);
        return R.ok(resourceId);
    }

    @Operation(
            summary = "获取笔记信息",
            description = """
                    - 用途：获取笔记资源详情、笔记元信息和作者展示信息。
                    - 请求：resourceId 指定笔记资源。
                    - 约束：当前用户必须已登录，且必须通过资源服务的资源详情权限校验；目标笔记必须存在。
                    - 处理：通过资源服务获取资源详情和当前用户动作集合，读取笔记信息，并尽量调用用户服务补充作者展示信息；作者展示信息补充失败时不阻断主响应。
                    - 失败：未登录 -> PermissionError.NOT_LOGIN；资源不存在 -> ResourceError.RESOURCE_NOT_FOUND；资源无查看权限 -> ResourceError.RESOURCE_PERMISSION_DENIED；笔记不存在 -> NoteError.NOTE_NOT_FOUND。
                    - 响应：返回资源信息、笔记信息和可用的作者展示信息。
                    """
    )
    @GetMapping("/getNoteInfo")
    public R<NoteInfoResponse> getNoteInfo(@RequestParam String resourceId) {
        // 若无权限将抛出异常，此处无需重复鉴权
        ResourceItemResponse resourceInfo = remoteResourceService.getResourceInfo(new ResourceInfoGetReqDTO(
                resourceId, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRoleMap()
        )).getData();
        NoteInfoBase noteInfo = noteService.getNoteInfo(resourceId);

        Map<Long, UserDisplayBase> authorsDisplay = null;
        try {
            authorsDisplay = remoteUserService.getUserDisplayInfo(noteInfo.getAuthors()).getData();
        } catch (Exception ignored){
        }

        NoteInfoResponse noteInfoResponse = NoteInfoResponse.builder()
                .resourceInfo(resourceInfo)
                .noteInfo(noteInfo)
                .authorsDisplay(authorsDisplay)
                .build();
        return R.ok(noteInfoResponse);
    }

    @Operation(
            summary = "分页查询笔记版本",
            description = """
                    - 用途：查询笔记的历史版本列表，供所有者查看版本轨迹。
                    - 请求：resourceId 指定笔记资源；page 和 size 控制分页。
                    - 约束：当前用户必须已登录，且必须是资源所有者。
                    - 处理：通过资源服务校验所有者身份后，按版本号倒序分页读取版本记录；不返回版本快照正文，不执行版本回退。
                    - 失败：未登录 -> PermissionError.NOT_LOGIN；当前用户不是资源所有者 -> NoteError.NOTE_PERMISSION_DENIED；权限计算发生未处理异常 -> CommonError.INTERNAL_ERROR。
                    - 响应：返回分页版本摘要列表和总数。
                    """
    )
    @GetMapping("/listNoteHistoryVersions")
    public R<PageR<NoteVersionListResponse>> listNoteHistoryVersions(
            @RequestParam String resourceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        ResourceCheckPermissionResDTO permission = remoteResourceService.checkResPermission(new ResourceCheckPermissionReqDTO(
                resourceId, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRoleMap()
        )).getData();
        if (permission.getResourceAccessRole() == ResourceAccessRole.OWNER){
            PageR<NoteVersionListResponse> noteVersionListResponses = noteVersionService.listVersions(resourceId, page, size);
            return R.ok(noteVersionListResponses);
        } else {
            throw new ServiceException(NOTE_PERMISSION_DENIED);
        }
    }

    @Operation(
            summary = "回退笔记版本",
            description = """
                    - 用途：预留给笔记所有者将笔记回退到指定历史版本。
                    - 请求：resourceId 指定笔记资源；version 指定目标版本号。
                    - 约束：当前用户必须已登录，且必须是资源所有者。
                    - 处理：当前接口仅完成所有者权限校验，实际回退逻辑尚未实现；不会修改笔记内容、版本记录或协同状态。
                    - 失败：未登录 -> PermissionError.NOT_LOGIN；当前用户不是资源所有者 -> NoteError.NOTE_PERMISSION_DENIED；权限计算发生未处理异常 -> CommonError.INTERNAL_ERROR。
                    - 响应：成功时返回空结果。
                    """
    )
    @PostMapping("/revertNote")
    public R<Void> revertToVersion(@RequestParam String resourceId, @RequestParam Long version) {
        ResourceCheckPermissionResDTO permission = remoteResourceService.checkResPermission(new ResourceCheckPermissionReqDTO(
                resourceId, SecurityContextHolder.getUserId(), SecurityContextHolder.getGroupRoleMap()
        )).getData();
        if (permission.getResourceAccessRole() == ResourceAccessRole.OWNER){
            // TODO: 待未来实现
        } else {
            throw new ServiceException(NOTE_PERMISSION_DENIED);
        }
        return R.ok();
    }
}
