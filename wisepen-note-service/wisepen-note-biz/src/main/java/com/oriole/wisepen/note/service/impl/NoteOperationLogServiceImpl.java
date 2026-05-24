package com.oriole.wisepen.note.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.note.api.domain.dto.res.NoteOperationLogResponse;
import com.oriole.wisepen.note.api.domain.mq.NoteOperationLogMessage;
import com.oriole.wisepen.note.domain.entity.NoteOperationLogEntity;
import com.oriole.wisepen.note.repository.NoteOperationLogRepository;
import com.oriole.wisepen.note.service.INoteOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteOperationLogServiceImpl implements INoteOperationLogService {

    private final NoteOperationLogRepository noteOperationLogRepository;

    @Override
    public void batchSave(NoteOperationLogMessage msg) {
        List<NoteOperationLogEntity> entities = msg.getEntries().stream().map(entry -> {
            NoteOperationLogEntity entity = NoteOperationLogEntity.builder()
                    .resourceId(msg.getResourceId())
                    .build();
            BeanUtil.copyProperties(entry, entity);
            return entity;
        }).toList();
        noteOperationLogRepository.saveAll(entities);
    }

    @Override
    public PageR<NoteOperationLogResponse> listOperationLogs(String resourceId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);
        Page<NoteOperationLogEntity> entityPage = noteOperationLogRepository.findByResourceIdOrderByTimestampDesc(resourceId, pageable);
        PageR<NoteOperationLogResponse> pageR = new PageR<>(entityPage.getTotalElements(), page, size);

        List<NoteOperationLogResponse> responses = entityPage.getContent().stream().map(entity -> {
            NoteOperationLogResponse response = new NoteOperationLogResponse();
            BeanUtil.copyProperties(entity, response);
            return response;
        }).toList();

        pageR.addAll(responses);
        return pageR;
    }

    @Override
    public void deleteAllOpLogsByResourceIds(List<String> resourceIds) {
        noteOperationLogRepository.deleteByResourceIdIn(resourceIds);
    }
}
