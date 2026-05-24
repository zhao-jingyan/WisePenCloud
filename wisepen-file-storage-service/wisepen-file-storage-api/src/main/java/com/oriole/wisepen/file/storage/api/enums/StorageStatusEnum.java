package com.oriole.wisepen.file.storage.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StorageStatusEnum {
    UPLOADING(1, "UPLOADING"),
    AVAILABLE(2, "AVAILABLE"),
    DELETED(3, "DELETED");

    @EnumValue
    @JsonValue
    private final int code;

    private final String value;
}
