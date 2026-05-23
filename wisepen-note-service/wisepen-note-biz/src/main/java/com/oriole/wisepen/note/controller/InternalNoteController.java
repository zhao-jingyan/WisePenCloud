package com.oriole.wisepen.note.controller;

import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.note.api.domain.dto.res.NoteSnapshotResponse;
import com.oriole.wisepen.note.api.feign.RemoteNoteService;
import com.oriole.wisepen.note.service.INoteVersionService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@Tag(name = "内部笔记服务", description = "供 Node.js 协同服务和其他微服务调用的内部接口")
@RestController
@RequestMapping("/internal/note")
@RequiredArgsConstructor
@Hidden
public class InternalNoteController implements RemoteNoteService {

    private final INoteVersionService noteVersionService;

    @Operation(summary = "获取最新快照")
    @GetMapping("/getNoteLatestVersion")
    @Override
    public R<NoteSnapshotResponse> getNoteLatestVersion(@RequestParam("resourceId") String resourceId) {
        return R.ok(noteVersionService.getLatestVersion(resourceId));
    }
}
