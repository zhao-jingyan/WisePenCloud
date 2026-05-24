package com.oriole.wisepen.resource.enums;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局全文搜索的资源范围三 Tab：
 * <ul>
 *   <li>{@link #ALL} —— 不按资源类型过滤</li>
 *   <li>{@link #DOCUMENT} —— 仅文档类资源（PDF/DOC/DOCX/PPT/PPTX/XLS/XLSX）</li>
 *   <li>{@link #NOTE} —— 仅笔记</li>
 * </ul>
 * <p>
 * {@link #includedResourceTypes()} 把 scope 展开为具体的 {@link ResourceType} 列表，始终排除 {@link ResourceType#UNKNOWN}。
 */
public enum SearchScope {
    ALL,
    DOCUMENT,
    NOTE;

    /** 文档类资源：所有非 NOTE / UNKNOWN 的资源类型 */
    private static final Set<ResourceType> DOCUMENT_TYPES = EnumSet.complementOf(
            EnumSet.of(ResourceType.NOTE, ResourceType.UNKNOWN));

    /**
     * 将 scope 展开为具体的 {@link ResourceType} 列表。
     *
     * @return ALL → 全部非 UNKNOWN；DOCUMENT → 文档类；NOTE → 仅笔记
     */
    public List<ResourceType> includedResourceTypes() {
        return switch (this) {
            case ALL -> Arrays.stream(ResourceType.values())
                    .filter(t -> t != ResourceType.UNKNOWN)
                    .collect(Collectors.toList());
            case DOCUMENT -> DOCUMENT_TYPES.stream().collect(Collectors.toList());
            case NOTE -> List.of(ResourceType.NOTE);
        };
    }
}
