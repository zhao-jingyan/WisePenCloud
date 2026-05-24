package com.oriole.wisepen.resource.controller;

import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.domain.enums.BusinessType;
import com.oriole.wisepen.common.log.annotation.Log;
import com.oriole.wisepen.common.security.annotation.CheckLogin;
import com.oriole.wisepen.resource.domain.dto.req.SearchQueryRequest;
import com.oriole.wisepen.resource.domain.dto.res.SearchHitItemResponse;
import com.oriole.wisepen.resource.service.ISearchQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全文搜索 Controller。
 * <p>
 * 作为前端"全局全文搜索"的唯一入口，按 {@code scope} (ALL/DOCUMENT/NOTE) 三 Tab + {@code page/size}
 * 滚动加载，转发给 {@link ISearchQueryService#globalSearch}。
 * <p>
 * 用户身份从 {@code SecurityContextHolder} 拿，Controller 不透传，避免越权。
 */
@Tag(name = "全文搜索", description = "跨资源类型的全文搜索（带 ACL 可见性过滤 + 高亮）")
@RestController
@RequestMapping("/resource/search")
@RequiredArgsConstructor
@CheckLogin
public class SearchController {

    private final ISearchQueryService searchQueryService;

    @Operation(summary = "全局全文搜索",
            description = "按 scope (ALL/DOCUMENT/NOTE) 三 Tab 过滤，按 page/size 滚动加载，命中字段携带 <em class=\"wp-highlight\"> 包裹")
    @Log(title = "全文搜索",
            businessType = BusinessType.SELECT,
            isSaveRequestData = false,
            isSaveResponseData = false)
    @PostMapping("/global")
    public R<PageR<SearchHitItemResponse>> globalSearch(@Validated @RequestBody SearchQueryRequest reqDTO) {
        return R.ok(searchQueryService.globalSearch(reqDTO));
    }
}
