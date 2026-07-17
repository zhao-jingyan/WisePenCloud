package com.oriole.wisepen.resource.service.impl;

import cn.hutool.core.util.IdUtil;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.resource.domain.InlineCommentCursor;
import com.oriole.wisepen.resource.domain.base.InlineCommentAnchor;
import com.oriole.wisepen.resource.domain.dto.req.InlineCommentCreateRequest;
import com.oriole.wisepen.resource.domain.dto.req.InlineCommentThreadCreateRequest;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentAuthorResponse;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentResponse;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentThreadChangeResponse;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentThreadChangesResponse;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentThreadListResponse;
import com.oriole.wisepen.resource.domain.dto.res.InlineCommentThreadResponse;
import com.oriole.wisepen.resource.domain.entity.InlineCommentThreadEntity;
import com.oriole.wisepen.resource.exception.ResourceError;
import com.oriole.wisepen.resource.repository.InlineCommentRepository;
import com.oriole.wisepen.resource.service.IInlineCommentService;
import com.oriole.wisepen.resource.util.InlineCommentCursorCodec;
import com.oriole.wisepen.user.api.domain.base.UserDisplayBase;
import com.oriole.wisepen.user.api.feign.RemoteUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class InlineCommentServiceImpl implements IInlineCommentService {

    private static final int CHANGE_PAGE_SIZE = 200;
    private static final int MAX_APPEND_ATTEMPTS = 128;

    private final InlineCommentRepository inlineCommentRepository;
    private final InlineCommentCursorCodec cursorCodec;
    private final RemoteUserService remoteUserService;

    @Override
    public InlineCommentThreadResponse createInlineCommentThread(
            InlineCommentThreadCreateRequest request,
            String operatorUserId
    ) {
        Optional<InlineCommentThreadEntity> existing = inlineCommentRepository
                .findByResourceIdAndCreateIdempotencyKey(request.getResourceId(), request.getIdempotencyKey());
        if (existing.isPresent()) {
            return toThreadResponse(existing.get());
        }

        Instant now = Instant.now();
        InlineCommentThreadEntity.Author author = loadAuthor(operatorUserId);
        InlineCommentThreadEntity.Item firstComment = InlineCommentThreadEntity.Item.builder()
                .commentId(IdUtil.fastSimpleUUID())
                .idempotencyKey(request.getIdempotencyKey())
                .authorId(operatorUserId)
                .author(author)
                .content(request.getContent())
                .createdAt(now)
                .revision(1L)
                .build();
        InlineCommentThreadEntity thread = InlineCommentThreadEntity.builder()
                .threadId(IdUtil.fastSimpleUUID())
                .resourceId(request.getResourceId())
                .createIdempotencyKey(request.getIdempotencyKey())
                .anchor(InlineCommentThreadEntity.Anchor.builder()
                        .start(request.getAnchor().getStart())
                        .end(request.getAnchor().getEnd())
                        .build())
                .quoteText(request.getQuoteText())
                .revision(1L)
                .items(List.of(firstComment))
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            InlineCommentThreadEntity inserted = inlineCommentRepository.insert(thread);
            log.info("inline comment thread created. resourceId={} threadId={} authorId={}",
                    inserted.getResourceId(), inserted.getThreadId(), operatorUserId);
            return toThreadResponse(inserted);
        } catch (DuplicateKeyException exception) {
            return inlineCommentRepository
                    .findByResourceIdAndCreateIdempotencyKey(request.getResourceId(), request.getIdempotencyKey())
                    .map(this::toThreadResponse)
                    .orElseThrow(() -> exception);
        }
    }

    @Override
    public InlineCommentResponse addInlineComment(
            String threadId,
            InlineCommentCreateRequest request,
            String operatorUserId
    ) {
        InlineCommentThreadEntity initialThread = getThreadEntity(threadId);
        Optional<InlineCommentThreadEntity.Item> existing = findByIdempotencyKey(
                initialThread,
                request.getIdempotencyKey()
        );
        if (existing.isPresent()) {
            return toInlineCommentResponse(existing.get());
        }

        InlineCommentThreadEntity.Author author = loadAuthor(operatorUserId);
        String commentId = IdUtil.fastSimpleUUID();
        Instant createdAt = Instant.now();
        InlineCommentThreadEntity currentThread = initialThread;
        for (int attempt = 0; attempt < MAX_APPEND_ATTEMPTS; attempt++) {
            existing = findByIdempotencyKey(currentThread, request.getIdempotencyKey());
            if (existing.isPresent()) {
                return toInlineCommentResponse(existing.get());
            }

            long nextRevision = currentThread.getRevision() + 1;
            InlineCommentThreadEntity.Item comment = InlineCommentThreadEntity.Item.builder()
                    .commentId(commentId)
                    .idempotencyKey(request.getIdempotencyKey())
                    .authorId(operatorUserId)
                    .author(author)
                    .content(request.getContent())
                    .createdAt(createdAt)
                    .revision(nextRevision)
                    .build();
            Optional<InlineCommentThreadEntity> updated = inlineCommentRepository.appendInlineComment(
                    threadId,
                    currentThread.getRevision(),
                    comment,
                    Instant.now()
            );
            if (updated.isPresent()) {
                InlineCommentThreadEntity.Item authoritativeComment = updated.get().getItems().stream()
                        .filter(item -> commentId.equals(item.getCommentId()))
                        .findFirst()
                        .orElseThrow(() -> new ServiceException(ResourceError.INLINE_COMMENT_REVISION_CONFLICT));
                log.info("inline comment created. resourceId={} threadId={} commentId={} authorId={} revision={}",
                        updated.get().getResourceId(), threadId, commentId, operatorUserId, nextRevision);
                return toInlineCommentResponse(authoritativeComment);
            }
            currentThread = getThreadEntity(threadId);
        }
        throw new ServiceException(ResourceError.INLINE_COMMENT_REVISION_CONFLICT);
    }

    @Override
    public InlineCommentThreadListResponse listInlineCommentThreads(String resourceId) {
        List<InlineCommentThreadEntity> threads = inlineCommentRepository.findAllByResourceId(resourceId);
        InlineCommentCursor cursor = threads.isEmpty()
                ? new InlineCommentCursor(Instant.EPOCH, "")
                : new InlineCommentCursor(threads.getFirst().getUpdatedAt(), threads.getFirst().getThreadId());
        return InlineCommentThreadListResponse.builder()
                .items(threads.stream().map(this::toThreadResponse).toList())
                .cursor(cursorCodec.encode(cursor))
                .build();
    }

    @Override
    public String getInlineCommentThreadResourceId(String threadId) {
        return getThreadEntity(threadId).getResourceId();
    }

    @Override
    public InlineCommentThreadResponse getInlineCommentThread(String threadId) {
        return toThreadResponse(getThreadEntity(threadId));
    }

    @Override
    public InlineCommentResponse getInlineComment(String threadId, String commentId) {
        InlineCommentThreadEntity thread = getThreadEntity(threadId);
        return thread.getItems().stream()
                .filter(item -> commentId.equals(item.getCommentId()))
                .findFirst()
                .map(this::toInlineCommentResponse)
                .orElseThrow(() -> new ServiceException(ResourceError.INLINE_COMMENT_NOT_FOUND));
    }

    @Override
    public InlineCommentThreadChangesResponse getInlineCommentChanges(String resourceId, String encodedCursor) {
        InlineCommentCursor cursor = StringUtils.hasText(encodedCursor)
                ? cursorCodec.decode(encodedCursor)
                : new InlineCommentCursor(Instant.EPOCH, "");
        List<InlineCommentThreadEntity> changedThreads = inlineCommentRepository.findChanges(
                resourceId,
                cursor,
                CHANGE_PAGE_SIZE
        );
        InlineCommentCursor nextCursor = changedThreads.isEmpty()
                ? cursor
                : new InlineCommentCursor(
                        changedThreads.getLast().getUpdatedAt(),
                        changedThreads.getLast().getThreadId()
                );
        return InlineCommentThreadChangesResponse.builder()
                .items(changedThreads.stream()
                        .map(thread -> InlineCommentThreadChangeResponse.builder()
                                .threadId(thread.getThreadId())
                                .revision(thread.getRevision())
                                .build())
                        .toList())
                .cursor(cursorCodec.encode(nextCursor))
                .build();
    }

    private InlineCommentThreadEntity getThreadEntity(String threadId) {
        return inlineCommentRepository.findById(threadId)
                .orElseThrow(() -> new ServiceException(ResourceError.INLINE_COMMENT_NOT_FOUND));
    }

    private Optional<InlineCommentThreadEntity.Item> findByIdempotencyKey(
            InlineCommentThreadEntity thread,
            String idempotencyKey
    ) {
        return thread.getItems().stream()
                .filter(item -> idempotencyKey.equals(item.getIdempotencyKey()))
                .findFirst();
    }

    private InlineCommentThreadEntity.Author loadAuthor(String operatorUserId) {
        Long userId = Long.valueOf(operatorUserId);
        R<Map<Long, UserDisplayBase>> response = remoteUserService.getUserDisplayInfo(List.of(userId));
        UserDisplayBase display = response.getData() == null ? null : response.getData().get(userId);
        String name = display != null && StringUtils.hasText(display.getNickname())
                ? display.getNickname()
                : display != null && StringUtils.hasText(display.getRealName())
                ? display.getRealName()
                : operatorUserId;
        return InlineCommentThreadEntity.Author.builder()
                .id(operatorUserId)
                .name(name)
                .avatar(display == null ? null : display.getAvatar())
                .build();
    }

    private InlineCommentThreadResponse toThreadResponse(InlineCommentThreadEntity thread) {
        return InlineCommentThreadResponse.builder()
                .threadId(thread.getThreadId())
                .resourceId(thread.getResourceId())
                .anchor(InlineCommentAnchor.builder()
                        .start(thread.getAnchor().getStart())
                        .end(thread.getAnchor().getEnd())
                        .build())
                .quoteText(thread.getQuoteText())
                .items(thread.getItems().stream().map(this::toInlineCommentResponse).toList())
                .revision(thread.getRevision())
                .createdAt(thread.getCreatedAt().toEpochMilli())
                .updatedAt(thread.getUpdatedAt().toEpochMilli())
                .build();
    }

    private InlineCommentResponse toInlineCommentResponse(InlineCommentThreadEntity.Item comment) {
        return InlineCommentResponse.builder()
                .commentId(comment.getCommentId())
                .authorId(comment.getAuthorId())
                .author(InlineCommentAuthorResponse.builder()
                        .id(comment.getAuthor().getId())
                        .name(comment.getAuthor().getName())
                        .avatar(comment.getAuthor().getAvatar())
                        .build())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt().toEpochMilli())
                .revision(comment.getRevision())
                .build();
    }
}
