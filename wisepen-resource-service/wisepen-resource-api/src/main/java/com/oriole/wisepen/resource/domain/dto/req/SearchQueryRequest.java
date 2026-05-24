package com.oriole.wisepen.resource.domain.dto.req;

import com.oriole.wisepen.resource.constant.SearchConstants;
import com.oriole.wisepen.resource.enums.SearchScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 全局全文搜索请求体。
 */
@Data
@Schema(description = "全局全文搜索请求体")
public class SearchQueryRequest {

    @NotBlank(message = "搜索关键词不能为空")
    @Schema(description = "搜索关键词", example = "操作系统", requiredMode = Schema.RequiredMode.REQUIRED)
    private String keyword;

    @Schema(description = "搜索范围（ALL/DOCUMENT/NOTE），默认 ALL", example = "ALL")
    private SearchScope scope = SearchScope.ALL;

    @Min(value = SearchConstants.MIN_PAGE_NUM, message = "页码不能小于1")
    @Schema(description = "页码（1-based）", example = "1")
    private int page = SearchConstants.DEFAULT_PAGE_NUM;

    @Min(value = SearchConstants.MIN_PAGE_SIZE, message = "每页条数不能小于1")
    @Max(value = SearchConstants.MAX_PAGE_SIZE, message = "每页条数不能大于100")
    @Schema(description = "每页条数 (1-100)", example = "20")
    private int size = SearchConstants.DEFAULT_PAGE_SIZE;
}
