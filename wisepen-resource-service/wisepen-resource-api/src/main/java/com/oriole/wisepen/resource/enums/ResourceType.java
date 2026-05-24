package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * 平台资源类型枚举
 * 覆盖所有受平台管理的资源类型，包括有实体文件的文档类型和无扩展名的笔记类型。
 */
@Getter
@AllArgsConstructor
public enum ResourceType {

    /** 无扩展名的笔记，由笔记服务管理，不经过文件上传流程。 */
    NOTE(1, "NOTE", "note"),
    PDF(2, "PDF", "pdf"),
    DOC(3, "DOC", "doc"),
    DOCX(4, "DOCX", "docx"),
    PPT(5, "PPT", "ppt"),
    PPTX(6, "PPTX", "pptx"),
    XLS(7, "XLS", "xls"),
    XLSX(8, "XLSX", "xlsx"),
    UNKNOWN(9, "UNKNOWN", "unknown");

    @EnumValue
    @JsonValue
    private final int code;

    private final String value;

    private final String extension;

    private static final Map<String, ResourceType> EXT_MAP = new HashMap<>();

    static {
        for (ResourceType item : values()) {
            EXT_MAP.put(item.extension, item);
        }
    }

    /**
     * 根据扩展名（大小写不敏感）查找对应枚举值。
     *
     * @param ext 扩展名或类型标识字符串，如 "docx"、"PDF"、"note"
     * @return 对应枚举值
     */
    @JsonCreator
    public static ResourceType fromExtension(String ext) {
        if (ext == null) {
            return null;
        }
        return EXT_MAP.get(ext.trim().toLowerCase());
    }
}
