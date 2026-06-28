package com.oriole.wisepen.note.service;

import com.oriole.wisepen.note.api.domain.dto.req.NoteCreateRequest;
import com.oriole.wisepen.note.api.domain.dto.req.NoteForkRequest;
import com.oriole.wisepen.note.domain.entity.NoteInfoEntity;

import java.util.List;

public interface INoteService {

    String createNote(NoteCreateRequest request, String userId);

    void deleteNotes(List<String> resourceIds);

    NoteInfoEntity getNoteInfo(String resourceId);

    String forkNote(NoteForkRequest request, String forkedResourceOwnerId);
}
