package com.oriole.wisepen.note.service;

import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.note.api.domain.dto.res.NoteSnapshotResponse;
import com.oriole.wisepen.note.api.domain.dto.res.NoteVersionListResponse;
import com.oriole.wisepen.note.api.domain.mq.NoteSnapshotMessage;

import java.util.List;

public interface INoteVersionService {

    void createVersion(NoteSnapshotMessage noteSnapshotMessage);

    NoteSnapshotResponse getLatestVersion(String resourceId);

    PageR<NoteVersionListResponse> listVersions(String resourceId, int page, int size);

    void deleteAllVersionsByResourceIds(List<String> resourceIds);
}