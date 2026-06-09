package com.oriole.wisepen.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.system.api.domain.dto.SysOperLogDTO;
import com.oriole.wisepen.system.api.domain.dto.res.SysOperLogResponse;
import com.oriole.wisepen.system.domain.entity.SysOperLogEntity;
import com.oriole.wisepen.system.mapper.SysOperLogMapper;
import com.oriole.wisepen.system.service.SysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SysOperLogServiceImpl implements SysOperLogService {

    @Autowired
    private SysOperLogMapper sysOperLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveLog(SysOperLogDTO dto) {
        SysOperLogEntity entity = BeanUtil.copyProperties(dto, SysOperLogEntity.class);
        int rows = sysOperLogMapper.insert(entity);
        return rows > 0;
    }

    @Override
    public PageR<SysOperLogResponse> listLogs(String operUrl, Long operUserId, LocalDateTime startTime, LocalDateTime endTime,
                                              Integer status, int page, int size) {

        LambdaQueryWrapper<SysOperLogEntity> queryWrapper = Wrappers.<SysOperLogEntity>lambdaQuery()
                .eq(operUrl != null, SysOperLogEntity::getOperUrl, operUrl)
                .eq(operUserId != null, SysOperLogEntity::getOperUserId,operUserId)
                .ge(startTime != null, SysOperLogEntity::getOperTime, startTime)
                .le(endTime != null, SysOperLogEntity::getOperTime, endTime)
                .eq(status != null, SysOperLogEntity::getStatus, status)
                .orderByDesc(SysOperLogEntity::getOperTime);
        IPage<SysOperLogEntity> result = sysOperLogMapper.selectPage(new Page<>(page, size), queryWrapper);

        PageR<SysOperLogResponse> pageR = new PageR<>(result.getTotal(), page, size);
        List<SysOperLogResponse> list = result.getRecords().stream()
                .map(entity -> BeanUtil.copyProperties(entity, SysOperLogResponse.class))
                .toList();
        pageR.addAll(list);

        return pageR;
    }
}
