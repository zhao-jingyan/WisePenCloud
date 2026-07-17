package com.oriole.wisepen.resource.service;

import com.oriole.wisepen.resource.domain.dto.req.InlineCommentCreateRequest;
import com.oriole.wisepen.resource.domain.dto.req.InlineCommentThreadCreateRequest;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentResponse;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentThreadChangesResponse;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentThreadListResponse;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentThreadResponse;

public interface IInlineCommentService {

    InlineCommentThreadResponse createInlineCommentThread(
            InlineCommentThreadCreateRequest request,
            String operatorUserId
    );

    InlineCommentResponse addInlineComment(
            String threadId,
            InlineCommentCreateRequest request,
            String operatorUserId
    );

    InlineCommentThreadListResponse listInlineCommentThreads(String resourceId);

    String getInlineCommentThreadResourceId(String threadId);

    InlineCommentThreadResponse getInlineCommentThread(String threadId);

    InlineCommentResponse getInlineComment(String threadId, String commentId);

    InlineCommentThreadChangesResponse getInlineCommentChanges(String resourceId, String cursor);
}
