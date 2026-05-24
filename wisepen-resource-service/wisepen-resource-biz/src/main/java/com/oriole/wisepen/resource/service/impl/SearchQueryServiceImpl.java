package com.oriole.wisepen.resource.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import com.oriole.wisepen.common.core.context.SecurityContextHolder;
import com.oriole.wisepen.common.core.domain.PageR;
import com.oriole.wisepen.common.core.domain.enums.GroupRoleType;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.resource.constant.SearchConstants;
import com.oriole.wisepen.resource.domain.dto.req.SearchQueryRequest;
import com.oriole.wisepen.resource.domain.dto.res.SearchHitItemResponse;
import com.oriole.wisepen.resource.domain.entity.ESIndexEntity;
import com.oriole.wisepen.resource.enums.ResourceType;
import com.oriole.wisepen.resource.enums.SearchScope;
import com.oriole.wisepen.resource.exception.SearchErrorCode;
import com.oriole.wisepen.resource.service.ISearchQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全文搜索查询服务实现：
 * <ul>
 *   <li><strong>关键词召回</strong>：multi_match 多字段加权（{@code resourceName^3, tags^2, content}）</li>
 *   <li><strong>ACL 可见性过滤</strong>：ES DSL 镜像资源域
 *       {@code CustomResourceItemRepository.findAccessibleResources()} 的 4-case OR
 *       + ADMIN/OWNER 短路 + 个人空间 ownerId 隔离</li>
 *   <li><strong>SearchScope 过滤</strong>：ALL 不过滤；DOCUMENT/NOTE 用 {@code terms} 命中对应资源类型集合</li>
 *   <li><strong>高亮</strong>：使用 {@code <em class="wp-highlight">…</em>}</li>
 *   <li><strong>上下文</strong>：{@code userId} / {@code groupRoleMap} 直接从 {@link SecurityContextHolder} 取，避免 Controller 透传越权</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchQueryServiceImpl implements ISearchQueryService {

    private final ElasticsearchOperations elasticsearchOperations;

    private static final IndexCoordinates INDEX = IndexCoordinates.of(SearchConstants.RESOURCE_INDEX_NAME);

    @Override
    public PageR<SearchHitItemResponse> globalSearch(SearchQueryRequest reqDTO) {
        // 1. 上下文
        Long userIdLong = SecurityContextHolder.getUserId();
        if (userIdLong == null) {
            throw new ServiceException(SearchErrorCode.ES_QUERY_BUILD_ERROR);
        }
        String userId = userIdLong.toString();
        Map<Long, GroupRoleType> groupRoleMap = SecurityContextHolder.getGroupRoleMap();

        SearchScope scope = reqDTO.getScope() == null ? SearchScope.ALL : reqDTO.getScope();
        int page = Math.max(reqDTO.getPage(), SearchConstants.MIN_PAGE_NUM);
        int size = Math.min(Math.max(reqDTO.getSize(), SearchConstants.MIN_PAGE_SIZE), SearchConstants.MAX_PAGE_SIZE);

        // 2. 拼 Query
        Query mainQuery;
        try {
            mainQuery = buildMainQuery(reqDTO.getKeyword(), scope, userId, groupRoleMap);
        } catch (Exception e) {
            log.error("ES query build failed keyword={} scope={}", reqDTO.getKeyword(), scope, e);
            throw new ServiceException(SearchErrorCode.ES_QUERY_BUILD_ERROR);
        }

        Pageable pageable = PageRequest.of(page - 1, size);

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(mainQuery)
                .withPageable(pageable)
                .withHighlightQuery(buildHighlightQuery())
                .withTrackTotalHits(true);

        NativeQuery nativeQuery = builder.build();

        // 3. 执行
        SearchHits<ESIndexEntity> searchHits;
        try {
            searchHits = elasticsearchOperations.search(nativeQuery, ESIndexEntity.class, INDEX);
        } catch (Exception e) {
            log.error("ES search failed keyword={} scope={}", reqDTO.getKeyword(), scope, e);
            throw new ServiceException(SearchErrorCode.ES_CONNECTION_ERROR);
        }

        // 4. 转 response
        List<SearchHitItemResponse> items = new ArrayList<>(searchHits.getSearchHits().size());
        for (SearchHit<ESIndexEntity> hit : searchHits.getSearchHits()) {
            items.add(toResponse(hit));
        }

        long total = searchHits.getTotalHits();
        PageR<SearchHitItemResponse> result = new PageR<>(total, page, size);
        result.addAll(items);
        log.info("search ok keyword={} scope={} userId={} total={} page={} size={}",
                reqDTO.getKeyword(), scope, userId, total, page, size);
        return result;
    }

    // ============== 主 Query 构造 ==============

    private Query buildMainQuery(String keyword,
                                 SearchScope scope,
                                 String userId,
                                 Map<Long, GroupRoleType> groupRoleMap) {

        BoolQuery.Builder bool = new BoolQuery.Builder();

        // [must] 关键词召回（multi_match + IK 智能切分）
        bool.must(Query.of(q -> q.multiMatch(m -> m
                .query(keyword == null ? "" : keyword.trim())
                .fields(List.of(SearchConstants.BOOSTED_SEARCH_FIELDS))
                .analyzer(SearchConstants.ANALYZER_IK_SMART)
        )));

        // [filter] SearchScope 类型过滤
        Query scopeFilter = buildScopeFilter(scope);
        if (scopeFilter != null) {
            bool.filter(scopeFilter);
        }

        // [filter] ACL 可见性过滤（镜像 §2.2 的 4-case OR + ADMIN/OWNER 短路 + 个人空间隔离）
        bool.filter(buildAclFilter(userId, groupRoleMap));

        return Query.of(q -> q.bool(bool.build()));
    }

    /**
     * Scope → terms 过滤；ALL 不过滤。
     */
    private Query buildScopeFilter(SearchScope scope) {
        if (scope == null || scope == SearchScope.ALL) {
            return null;
        }
        List<String> exts = scope.includedResourceTypes().stream()
                .map(ResourceType::getExtension)
                .toList();
        if (exts.isEmpty()) {
            return null;
        }
        return Query.of(q -> q.terms(t -> t
                .field(SearchConstants.FIELD_RESOURCE_TYPE)
                .terms(tv -> tv.value(exts.stream()
                        .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                        .toList()))));
    }

    /**
     * 镜像 {@code findAccessibleResources()} 的 4-case OR + ADMIN/OWNER 短路。
     * <p>
     * 因 {@code computedGroupAcls} 投影端已将 {@code userMasks} 折叠为单一 {@code specifiedUsers}
     * （行为与 {@code isDiscover} <strong>相反</strong>的例外用户，详见
     * {@link com.oriole.wisepen.resource.domain.entity.ESIndexEntity.ComputedGroupAclProjection}），
     * 这里原本独立的 Case C / Case D 也合并成单一表达式：
     * <pre>
     * should:
     *   - term ownerId = userId                                                  (Case A)
     *   - term specifiedDiscoverUsers = userId                                   (Case B)
     *   - nested computedGroupAcls:
     *       should:
     *         - terms groupId in adminOwnerGroupIds                              (ADMIN/OWNER 短路)
     *         - bool { terms groupId in memberGroupIds,
     *                  term isDiscover = true,
     *                  must_not term specifiedUsers = userId }                   (默认放行，未被拉黑)
     *         - bool { terms groupId in memberGroupIds,
     *                  term isDiscover = false,
     *                  term specifiedUsers = userId }                            (默认拒绝，但被白名单)
     * </pre>
     * 两个 bool 子句等价于 {@code isDiscover XOR (userId ∈ specifiedUsers) = true}，
     * 在 ES 端用 OR-of-AND 表达比 script 更友好且能命中倒排索引。
     */
    private Query buildAclFilter(String userId, Map<Long, GroupRoleType> groupRoleMap) {
        BoolQuery.Builder acl = new BoolQuery.Builder();
        acl.minimumShouldMatch("1");

        // Case A: 自己的资源
        acl.should(Query.of(q -> q.term(t -> t
                .field(SearchConstants.FIELD_OWNER_ID)
                .value(userId))));

        // Case B: 资源级 DISCOVER 特权
        acl.should(Query.of(q -> q.term(t -> t
                .field(SearchConstants.FIELD_SPECIFIED_DISCOVER_USERS)
                .value(userId))));

        // Case C + D + ADMIN/OWNER 短路：合并到一个 nested 查询
        Set<String> adminOwnerGroupIds = groupRoleMap == null ? Collections.emptySet()
                : groupRoleMap.entrySet().stream()
                .filter(e -> e.getValue() == GroupRoleType.ADMIN || e.getValue() == GroupRoleType.OWNER)
                .map(e -> e.getKey().toString())
                .collect(Collectors.toSet());
        Set<String> memberGroupIds = groupRoleMap == null ? Collections.emptySet()
                : groupRoleMap.keySet().stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());

        if (!memberGroupIds.isEmpty()) {
            BoolQuery.Builder nestedBool = new BoolQuery.Builder().minimumShouldMatch("1");

            // ADMIN/OWNER 短路
            if (!adminOwnerGroupIds.isEmpty()) {
                nestedBool.should(Query.of(q -> q.terms(t -> t
                        .field(SearchConstants.FIELD_COMPUTED_GROUP_ACLS_GROUP_ID)
                        .terms(tv -> tv.value(adminOwnerGroupIds.stream()
                                .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                .toList())))));
            }

            // 默认放行分支：isDiscover=true AND userId NOT IN specifiedUsers（黑名单未命中）
            nestedBool.should(Query.of(q -> q.bool(b -> b
                    .must(Query.of(q1 -> q1.terms(t -> t
                            .field(SearchConstants.FIELD_COMPUTED_GROUP_ACLS_GROUP_ID)
                            .terms(tv -> tv.value(memberGroupIds.stream()
                                    .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                    .toList())))))
                    .must(Query.of(q2 -> q2.term(t -> t
                            .field(SearchConstants.FIELD_COMPUTED_GROUP_ACLS_IS_DISCOVER)
                            .value(true))))
                    .mustNot(Query.of(q3 -> q3.term(t -> t
                            .field(SearchConstants.FIELD_COMPUTED_GROUP_ACLS_SPECIFIED_USERS)
                            .value(userId))))
            )));

            // 默认拒绝分支：isDiscover=false AND userId IN specifiedUsers（白名单命中）
            nestedBool.should(Query.of(q -> q.bool(b -> b
                    .must(Query.of(q1 -> q1.terms(t -> t
                            .field(SearchConstants.FIELD_COMPUTED_GROUP_ACLS_GROUP_ID)
                            .terms(tv -> tv.value(memberGroupIds.stream()
                                    .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                                    .toList())))))
                    .must(Query.of(q2 -> q2.term(t -> t
                            .field(SearchConstants.FIELD_COMPUTED_GROUP_ACLS_IS_DISCOVER)
                            .value(false))))
                    .must(Query.of(q3 -> q3.term(t -> t
                            .field(SearchConstants.FIELD_COMPUTED_GROUP_ACLS_SPECIFIED_USERS)
                            .value(userId))))
            )));

            Query nestedBoolQuery = Query.of(q -> q.bool(nestedBool.build()));

            acl.should(Query.of(q -> q.nested(n -> n
                    .path(SearchConstants.FIELD_COMPUTED_GROUP_ACLS)
                    .scoreMode(ChildScoreMode.None)
                    .query(nestedBoolQuery))));
        }

        return Query.of(q -> q.bool(acl.build()));
    }

    // ============== 高亮 ==============

    private HighlightQuery buildHighlightQuery() {
        HighlightFieldParameters fieldParams = HighlightFieldParameters.builder()
                .withFragmentSize(SearchConstants.HIGHLIGHT_FRAGMENT_SIZE)
                .withNumberOfFragments(SearchConstants.HIGHLIGHT_MAX_FRAGMENTS)
                .build();

        HighlightField nameField = new HighlightField(SearchConstants.FIELD_RESOURCE_NAME, fieldParams);
        HighlightField contentField = new HighlightField(SearchConstants.FIELD_CONTENT, fieldParams);
        HighlightField tagsField = new HighlightField(SearchConstants.FIELD_TAGS, fieldParams);

        HighlightParameters params = HighlightParameters.builder()
                .withPreTags(SearchConstants.HIGHLIGHT_PRE_TAG)
                .withPostTags(SearchConstants.HIGHLIGHT_POST_TAG)
                .build();

        Highlight highlight = new Highlight(params, List.of(nameField, contentField, tagsField));
        return new HighlightQuery(highlight, ESIndexEntity.class);
    }

    // ============== 结果映射 ==============

    private SearchHitItemResponse toResponse(SearchHit<ESIndexEntity> hit) {
        ESIndexEntity content = hit.getContent();

        // 高亮覆写：resourceName / content / tags
        Map<String, List<String>> highlights = hit.getHighlightFields();

        String resourceName = pickHighlight(highlights, SearchConstants.FIELD_RESOURCE_NAME, content.getResourceName());
        String highlightContent = joinHighlight(highlights, SearchConstants.FIELD_CONTENT);
        List<String> tags = pickTagsHighlight(highlights, content.getTags());

        return SearchHitItemResponse.builder()
                .resourceId(content.getResourceId())
                .resourceType(ResourceType.fromExtension(content.getResourceType()))
                .resourceName(resourceName)
                .highlightContent(highlightContent)
                .updateTime(content.getUpdateTime())
                .tags(tags)
                .build();
    }

    /** 高亮命中：单值字段（如 resourceName）取第一个片段，未命中则回退到原始值。 */
    private String pickHighlight(Map<String, List<String>> highlights, String field, String fallback) {
        if (highlights != null) {
            List<String> frags = highlights.get(field);
            if (frags != null && !frags.isEmpty()) {
                return frags.get(0);
            }
        }
        return fallback;
    }

    /** 高亮命中：长正文字段（content）拼接多个 fragment，未命中则返回 null。 */
    private String joinHighlight(Map<String, List<String>> highlights, String field) {
        if (highlights == null) {
            return null;
        }
        List<String> frags = highlights.get(field);
        if (frags == null || frags.isEmpty()) {
            return null;
        }
        return frags.stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(SearchConstants.HIGHLIGHT_FRAGMENT_SEPARATOR));
    }

    /** 高亮命中：多值字段（tags），命中的标签替换为带高亮版本，未命中的保留原值。 */
    private List<String> pickTagsHighlight(Map<String, List<String>> highlights, List<String> originalTags) {
        if (originalTags == null || originalTags.isEmpty()) {
            return Collections.emptyList();
        }
        if (highlights == null) {
            return originalTags;
        }
        List<String> frags = highlights.get(SearchConstants.FIELD_TAGS);
        if (frags == null || frags.isEmpty()) {
            return originalTags;
        }
        // ES 高亮按文本片段返回，可能不是 1:1 对应原 tag。统一策略：覆盖到对应文本的 tag 上，剩余保留原 tag。
        List<String> highlighted = new ArrayList<>(originalTags);
        for (String frag : frags) {
            String plain = frag
                    .replace(SearchConstants.HIGHLIGHT_PRE_TAG, "")
                    .replace(SearchConstants.HIGHLIGHT_POST_TAG, "");
            for (int i = 0; i < highlighted.size(); i++) {
                if (plain.equals(highlighted.get(i))) {
                    highlighted.set(i, frag);
                    break;
                }
            }
        }
        return highlighted;
    }
}
