package com.oriole.wisepen.note.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.note.api.domain.base.NoteInfoBase;
import com.oriole.wisepen.note.api.domain.dto.req.NoteCreateRequest;
import com.oriole.wisepen.note.api.domain.enums.VersionType;
import com.oriole.wisepen.note.domain.entity.NoteInfoEntity;
import com.oriole.wisepen.note.domain.entity.NoteVersionEntity;
import com.oriole.wisepen.note.exception.NoteError;
import com.oriole.wisepen.note.repository.NoteDocumentRepository;
import com.oriole.wisepen.note.repository.NoteVersionRepository;
import com.oriole.wisepen.note.service.INoteOperationLogService;
import com.oriole.wisepen.note.service.INoteService;
import com.oriole.wisepen.note.service.INoteVersionService;
import com.oriole.wisepen.resource.domain.dto.ResourceCreateReqDTO;
import com.oriole.wisepen.resource.domain.mq.ResourceForkMessage;
import com.oriole.wisepen.resource.enums.ResourceType;
import com.oriole.wisepen.resource.feign.RemoteResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.Binary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements INoteService {

    private final NoteDocumentRepository noteDocumentRepository;

    private final NoteVersionRepository noteVersionRepository;
    private final INoteVersionService noteVersionService;
    private final INoteOperationLogService noteOperationLogService;
    private final RemoteResourceService remoteResourceService;

    @Override
    public String createNote(NoteCreateRequest request, String userId) {

        // 向 resource 服务注册Note资源
        String resourceId;
        try {
            resourceId = remoteResourceService.createResource(
                    ResourceCreateReqDTO.builder()
                            .resourceName(request.getTitle())
                            .resourceType(ResourceType.NOTE)
                            .ownerId(userId)
                            .build()
            ).getData();
        } catch (Exception e) {
            log.error("note resource register failed. dependency=resourceService", e);
            throw new ServiceException(NoteError.NOTE_REGISTER_RESOURCE_FAILED, e.getMessage());
        }

        List<Long> authors = new ArrayList<>();
        authors.add(Long.valueOf(userId));

        NoteInfoEntity doc = NoteInfoEntity.builder()
                .resourceId(resourceId)
                .lastUpdatedAt(LocalDateTime.now())
                .authors(authors)
                .build();
        noteDocumentRepository.save(doc);
        return resourceId;
    }

    @Override
    @Transactional
    public void deleteNotes(List<String> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return;
        }
        // 移除所有内容
        noteDocumentRepository.deleteByResourceIdIn(resourceIds);

        noteVersionService.deleteAllVersionsByResourceIds(resourceIds);
        noteOperationLogService.deleteAllOpLogsByResourceIds(resourceIds);
    }

    @Override
    public NoteInfoBase getNoteInfo(String resourceId) {
        NoteInfoEntity noteInfoEntity = noteDocumentRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new ServiceException(NoteError.NOTE_NOT_FOUND));
        return BeanUtil.copyProperties(noteInfoEntity, NoteInfoBase.class);
    }

    @Override
    public void forkNote(ResourceForkMessage msg) {
        NoteInfoEntity sourceInfo = noteDocumentRepository.findByResourceId(msg.getSourceResourceId())
                .orElseThrow(() -> new ServiceException(NoteError.NOTE_NOT_FOUND));

        List<NoteVersionEntity> sourceVersions = new ArrayList<>();
        Long latestFullVersion = 0L;
        Optional<NoteVersionEntity> latestFull = noteVersionRepository
                .findFirstByResourceIdAndTypeAndVersionLessThanEqualOrderByVersionDesc(
                        msg.getSourceResourceId(), VersionType.FULL, msg.getVersion());
        if (latestFull.isPresent()) {
            NoteVersionEntity fullVersion = latestFull.get();
            sourceVersions.add(fullVersion);
            latestFullVersion = fullVersion.getVersion();
        }
        sourceVersions.addAll(noteVersionRepository
                .findByResourceIdAndVersionGreaterThanAndVersionLessThanEqualAndTypeOrderByVersionAsc(
                        msg.getSourceResourceId(), latestFullVersion, msg.getVersion(), VersionType.DELTA));

        String targetResourceId;
        try {
            targetResourceId = remoteResourceService.createResource(ResourceCreateReqDTO.builder()
                    .resourceName(msg.getResourceName())
                    .resourceType(ResourceType.NOTE)
                    .ownerId(msg.getBuyerId().toString())
                    .preview(msg.getPreview())
                    .size(msg.getSize())
                    .build()).getData();
        } catch (Exception e) {
            log.error("noteResourceCreate failed forkTaskId={} sourceResourceId={}",
                    msg.getForkTaskId(), msg.getSourceResourceId(), e);
            throw new ServiceException(NoteError.NOTE_REGISTER_RESOURCE_FAILED, e.getMessage());
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            List<Long> authors = List.of(msg.getBuyerId());
            noteDocumentRepository.save(NoteInfoEntity.builder()
                    .resourceId(targetResourceId)
                    .lastUpdatedAt(now)
                    .authors(authors)
                    .plainText(sourceInfo.getPlainText())
                    .build());

            if (!sourceVersions.isEmpty()) {
                List<NoteVersionEntity> targetVersions = new ArrayList<>();
                for (NoteVersionEntity sourceVersion : sourceVersions) {
                    targetVersions.add(NoteVersionEntity.builder()
                            .resourceId(targetResourceId)
                            .version(sourceVersion.getVersion())
                            .type(sourceVersion.getType())
                            .label(sourceVersion.getLabel())
                            .createdAt(now)
                            .createdBy(authors)
                            .data(new Binary(sourceVersion.getData().getData()))
                            .build());
                }
                noteVersionRepository.saveAll(targetVersions);
            }
            log.info("noteFork created forkTaskId={} sourceResourceId={} resourceId={} version={}",
                    msg.getForkTaskId(), msg.getSourceResourceId(), targetResourceId, msg.getVersion());
        } catch (Exception e) {
            noteVersionRepository.deleteByResourceIdIn(List.of(targetResourceId));
            noteDocumentRepository.deleteById(targetResourceId);
            log.warn("noteFork compensated forkTaskId={} sourceResourceId={} resourceId={}",
                    msg.getForkTaskId(), msg.getSourceResourceId(), targetResourceId, e);
            throw new ServiceException(NoteError.NOTE_FORK_FAILED, e.getMessage());
        }
    }
}
