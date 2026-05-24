package com.oriole.wisepen.note.service;

import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.note.api.domain.dto.res.NoteOperationLogResponse;
import com.oriole.wisepen.note.api.domain.mq.NoteOperationLogMessage;

import java.util.List;

public interface INoteOperationLogService {

    void batchSave(NoteOperationLogMessage message);

    PageR<NoteOperationLogResponse> listOperationLogs(String resourceId, int page, int size);

    void deleteAllOpLogsByResourceIds(List<String> resourceId);
}
