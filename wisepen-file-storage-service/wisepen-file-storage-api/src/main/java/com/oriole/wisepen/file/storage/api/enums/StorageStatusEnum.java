package com.oriole.wisepen.file.storage.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储提供商枚举
 */
@Getter
@AllArgsConstructor
public enum StorageStatusEnum implements WisePenEnum {
    UPLOADING(1, "UPLOADING", "UPLOADING"),
    AVAILABLE(2, "AVAILABLE", "AVAILABLE"),
    DELETED(3, "DELETED", "DELETED");

    @EnumValue
    private final Integer code;
    private final String value;
    private final String desc;
}
