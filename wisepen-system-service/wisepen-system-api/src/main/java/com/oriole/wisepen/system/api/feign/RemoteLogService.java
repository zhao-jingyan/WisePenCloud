package com.oriole.wisepen.system.api.feign;

import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.system.api.domain.dto.SysOperLogDTO;
import com.oriole.wisepen.system.api.domain.dto.res.SysOperLogResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@FeignClient(contextId = "remoteLogService", value = "wisepen-system-service")
public interface RemoteLogService {

    @PostMapping("/system/log/saveLog")
    R<Boolean> saveLog(@RequestBody SysOperLogDTO sysOperLog);

    @PostMapping("/system/log/listLogs")
    R<PageR<SysOperLogResponse>> listLogs(@RequestParam String operUrl, @RequestParam Long operUserId, @RequestParam LocalDateTime startTime, @RequestParam LocalDateTime endTime,
                                          @RequestParam Integer status, @RequestParam int page, @RequestParam  int size);
}
