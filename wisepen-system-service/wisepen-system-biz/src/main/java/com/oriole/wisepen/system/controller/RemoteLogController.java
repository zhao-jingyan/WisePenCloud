package com.oriole.wisepen.system.controller;

import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.system.api.domain.dto.SysOperLogDTO;
import com.oriole.wisepen.system.api.domain.dto.res.SysOperLogResponse;
import com.oriole.wisepen.system.service.SysOperLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


@Tag(name = "内部 - 日志", description = "供业务微服务写入系统操作日志")
@RestController
@RequestMapping("/system/log") // 注意这里的基础路径
public class RemoteLogController {

    @Autowired
    private SysOperLogService sysOperLogService;

    /**
     * 保存日志
     */
    @Operation(
            summary = "内部保存操作日志",
            description = """
                    - 用途：供业务微服务通过内部调用写入系统操作日志。
                    - 请求：请求体携带操作模块、业务类型、请求信息、响应信息和操作者等日志字段。
                    - 约束：调用方必须通过内部服务调用边界；日志 DTO 字段由调用方按统一日志模型提供。
                    - 处理：将 DTO 转换为系统操作日志实体并插入日志表；不校验业务对象是否存在，不回写调用方业务状态。
                    - 失败：日志写入发生未处理异常 -> CommonError.INTERNAL_ERROR。
                    - 响应：返回日志写入是否成功。
                    """
    )
    @PostMapping("/saveLog")
    public R<Boolean> saveLog(@RequestBody SysOperLogDTO dto) {
        return R.ok(sysOperLogService.saveLog(dto));
    }

    @Operation(summary = "内部查询操作日志")
    @GetMapping("/listLogs")
    R<PageR<SysOperLogResponse>> listLogs(@RequestParam String operUrl, @RequestParam Long operUserId,
                                          @RequestParam LocalDateTime startTime, @RequestParam LocalDateTime endTime,
                                          @RequestParam Integer status, @RequestParam int page, @RequestParam int size) {
        return R.ok(sysOperLogService.listLogs(operUrl, operUserId, startTime, endTime, status, page, size));
    }
}
