package com.oriole.wisepen.resource.service;

import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.resource.domain.dto.req.SearchQueryRequest;
import com.oriole.wisepen.resource.domain.dto.res.SearchHitItemResponse;

/**
 * 全文搜索查询服务。
 * <p>
 * 内部组装"关键词召回 + 可见性过滤 + 类型过滤 + 高亮"的 Native Query 并执行查询，
 * 镜像资源域 {@code CustomResourceItemRepository.findAccessibleResources()} 的过滤语义，
 * 让"列表页"和"搜索页"看到的可见集合天然一致。
 */
public interface ISearchQueryService {

    /**
     * 全局全文搜索入口。
     *
     * @param reqDTO 关键词 / scope / page / size
     * @return 分页结果，命中项的 resourceName / highlightContent 已带高亮包裹
     */
    PageR<SearchHitItemResponse> globalSearch(SearchQueryRequest reqDTO);
}
