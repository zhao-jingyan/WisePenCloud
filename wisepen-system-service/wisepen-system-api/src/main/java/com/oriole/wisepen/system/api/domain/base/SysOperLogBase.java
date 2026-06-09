package com.oriole.wisepen.system.api.domain.base;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysOperLogBase {
    private String title;
    private Integer businessType;
    private String method;
    private String reqMethod;
    private Long operUserId;
    private String operUrl;
    private String operIp;
    private String operParam;
    private String jsonResult;
    private Integer status;
    private String errorMsg;
    private LocalDateTime operTime;
    private Long costTime;
}
