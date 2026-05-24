package com.oriole.wisepen.resource.domain.dto.res;

import com.oriole.wisepen.resource.enums.ResourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 全文搜索单条命中结果。
 * <p>
 * {@code resourceName} 与 {@code highlightContent} 可能携带高亮包裹标签（{@code <em class="wp-highlight">}），
 * 前端使用 {@code v-html} 渲染即可，已在写入 ES 前对原文做了 HTML escape。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "搜索命中结果")
public class SearchHitItemResponse {

    @Schema(description = "资源 ID")
    private String resourceId;

    @Schema(description = "资源类型")
    private ResourceType resourceType;

    @Schema(description = "资源名称（可能携带高亮 <em class=\"wp-highlight\">…</em>）")
    private String resourceName;

    @Schema(description = "正文高亮片段（多片用 ... 拼接）")
    private String highlightContent;

    @Schema(description = "最近更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "标签名列表（命中时可能携带高亮）")
    private List<String> tags;
}
