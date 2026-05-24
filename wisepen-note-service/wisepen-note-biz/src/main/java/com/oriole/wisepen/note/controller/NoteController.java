package com.oriole.wisepen.note.controller;

import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.exception.ServiceException;
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
@Tag(name = "笔记服务", description = "笔记的创建、删除、版本管理与操作日志")
@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
@CheckLogin
public class NoteController {

    private final INoteService noteService;
    private final INoteVersionService noteVersionService;
    private final RemoteResourceService remoteResourceService;
    private final RemoteUserService remoteUserService;

    @Operation(summary = "创建笔记")
    @PostMapping("/addNote")
    public R<String> createNote(@Validated @RequestBody NoteCreateRequest request) {
        String userId = SecurityContextHolder.getUserId().toString();
        String resourceId = noteService.createNote(request, userId);
        return R.ok(resourceId);
    }

    @Operation(summary = "获取笔记信息")
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

    @Operation(summary = "查询版本历史列表")
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

    @Operation(summary = "回退到指定版本")
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
