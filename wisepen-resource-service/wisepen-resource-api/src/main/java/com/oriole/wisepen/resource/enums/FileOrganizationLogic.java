package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FileOrganizationLogic {
    FOLDER(1, "FOLDER"),
    TAG(2, "TAG");

    @EnumValue
    @JsonValue
    private final int code;

    private final String value;

    // 兼容此前传值
    @JsonCreator
    public static FileOrganizationLogic fromValue(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        for (FileOrganizationLogic item : values()) {
            if (item.value.equalsIgnoreCase(text) || item.name().equalsIgnoreCase(text)) {
                return item;
            }
        }
        try {
            int code = Integer.parseInt(text);
            for (FileOrganizationLogic item : values()) {
                if (item.code == code) {
                    return item;
                }
            }
        } catch (NumberFormatException ignored) {
            // Continue to error below.
        }
        throw new IllegalArgumentException("Unknown FileOrganizationLogic: " + value);
    }
}
