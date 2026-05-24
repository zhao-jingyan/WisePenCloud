package com.oriole.wisepen.document.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentStatusEnum {

    UPLOADING(0, "UPLOADING"),
    UPLOADED(1, "UPLOADED"),
    CONVERTING_AND_PARSING(2, "CONVERTING_AND_PARSING"),
    REGISTERING_RES(3, "REGISTERING_RES"),
    READY(4, "READY"),

    /** 上传超时：OSS 回调在预期时限内未收到，需人工或自动重试 */
    TRANSFER_TIMEOUT(-1, "TRANSFER_TIMEOUT"),
    REGISTERING_RES_TIMEOUT(-2, "REGISTERING_RES_TIMEOUT"),
    FAILED(-3, "FAILED");

    @EnumValue
    @JsonValue
    private final int code;

    private final String value;
}
