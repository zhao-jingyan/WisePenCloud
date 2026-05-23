package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.Getter;

@Getter
public enum ResourceSortBy implements WisePenEnum {
    UPDATE_TIME(1, "updateTime", "UPDATE_TIME", "updateTime"),
    CREATE_TIME(2, "createTime", "CREATE_TIME", "createTime"),
    NAME(3, "resourceName", "NAME", "resourceName"),
    SIZE(4, "size", "SIZE", "size");

    @EnumValue
    private final Integer code;
    private final String value;
    private final String desc;
    // 对应的 MongoDB 实体字段名
    private final String dbField;

    ResourceSortBy(Integer code, String value, String desc, String dbField) {
        this.code = code;
        this.value = value;
        this.desc = desc;
        this.dbField = dbField;
    }
}
