package com.oriole.wisepen.note.controller;

import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.note.api.domain.dto.res.NoteOperationLogResponse;
import com.oriole.wisepen.note.service.INoteOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "笔记日志服务", description = "查看笔记的细粒度操作日志")
@RestController
@RequestMapping("/note/log")
@RequiredArgsConstructor
@CheckLogin
public class OpLogController {

    private final INoteOperationLogService noteOperationLogService;

    @Operation(summary = "查询操作日志")
    @GetMapping("/getNoteOpLogs")
    public R<PageR<NoteOperationLogResponse>> listNoteOperationLogs(
            @RequestParam String resourceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(noteOperationLogService.listOperationLogs(resourceId, page, size));
    }
}
