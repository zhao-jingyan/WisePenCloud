package com.oriole.wisepen.system.service;

import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.system.api.domain.dto.SysOperLogDTO;
import com.oriole.wisepen.system.api.domain.dto.res.SysOperLogResponse;

import java.time.LocalDateTime;

public interface SysOperLogService {

    boolean saveLog(SysOperLogDTO dto);

    PageR<SysOperLogResponse> listLogs(String operUrl, Long operUserId, LocalDateTime startTime, LocalDateTime endTime,
                                       Integer status, int page, int size);
}
