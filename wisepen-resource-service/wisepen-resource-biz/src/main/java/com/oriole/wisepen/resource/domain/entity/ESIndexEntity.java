package com.oriole.wisepen.resource.domain.entity;

import com.oriole.wisepen.resource.constant.SearchConstants;
import com.oriole.wisepen.resource.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 全文搜索 ES 索引文档。
 * <p>
 * 文档主键 {@link #id} 使用 {@code <resourceType>_<resourceId>}（小写）格式，
 * 见 {@link #generateEsId(ResourceType, String)}，避免不同资源类型的 ID 冲突。
 * <p>
 * ACL 字段语义与 Mongo {@code ResourceItemEntity} 一一对应，但<strong>只投影 DISCOVER 位</strong>，
 * 详见 {@link com.oriole.wisepen.resource.util.ESIndexBuilder} 与 FULL_TEXT_SEARCH.md §2.4。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = SearchConstants.RESOURCE_INDEX_NAME, createIndex = true)
public class ESIndexEntity {

    /** ES 文档主键：{resourceType}_{resourceId} */
    @Id
    private String id;

    /** 资源业务 ID */
    @Field(type = FieldType.Keyword)
    private String resourceId;

    /** 资源类型，存 {@code ResourceType.getExtension()} 小写值 */
    @Field(type = FieldType.Keyword)
    private String resourceType;

    /** 资源名（IK 双分词器） */
    @Field(type = FieldType.Text,
            analyzer = SearchConstants.ANALYZER_IK_MAX_WORD,
            searchAnalyzer = SearchConstants.ANALYZER_IK_SMART)
    private String resourceName;

    /** 正文（IK 双分词器） */
    @Field(type = FieldType.Text,
            analyzer = SearchConstants.ANALYZER_IK_MAX_WORD,
            searchAnalyzer = SearchConstants.ANALYZER_IK_SMART)
    private String content;

    /** 标签名列表（IK 双分词器；ESIndexBuilder 内已把 tagId → tagName 翻译完成） */
    @Field(type = FieldType.Text,
            analyzer = SearchConstants.ANALYZER_IK_MAX_WORD,
            searchAnalyzer = SearchConstants.ANALYZER_IK_SMART)
    private List<String> tags;

    // ============== ACL 投影字段（DISCOVER 位） ==============

    /** 资源所有者 ID（§2.2 Case A + 个人空间隔离） */
    @Field(type = FieldType.Keyword)
    private String ownerId;

    /** specifiedUsersGrantedActionsMask 中 DISCOVER 位为 1 的 userId 集合（§2.2 Case B） */
    @Field(type = FieldType.Keyword)
    private List<String> specifiedDiscoverUsers;

    /** 按"绑定组"维度编码的 ACL 投影；个人空间组不入此列表（由 ownerId 兜底） */
    @Field(type = FieldType.Nested)
    private List<ComputedGroupAclProjection> computedGroupAcls;

    @Field(type = FieldType.Date,
            format = {},
            pattern = SearchConstants.ES_DATE_FORMAT_PATTERN)
    private LocalDateTime updateTime;

    /**
     * 根据 (资源类型, 资源 ID) 计算 ES 文档 ID。
     * 例如 {@code (PDF, "abc") -> "pdf_abc"}，避免多类型 ID 冲突。
     */
    public static String generateEsId(ResourceType resourceType, String resourceId) {
        if (resourceType == null || resourceId == null) {
            return null;
        }
        return resourceType.getExtension() + "_" + resourceId;
    }

    /**
     * 单个绑定组的 ACL 投影：把 Mongo 端的 {@code (baseMask, userMasks)} 折叠成
     * {@code (isDiscover, specifiedUsers)} 两个 ES 友好的字段。
     * <p>
     * <strong>语义</strong>：
     * <ul>
     *   <li>{@code isDiscover}：该组默认能否看到资源（即 {@code baseMask & DISCOVER != 0}）。</li>
     *   <li>{@code specifiedUsers}：{@code userMasks} 中 DISCOVER 位与 {@code isDiscover}
     *       <strong>相反</strong>的 userId 集合，也就是组默认行为的"例外名单"。</li>
     * </ul>
     * <p>
     * <strong>为什么不用白/黑两份名单</strong>：在 ACL 重算阶段，{@code userMasks} 与 {@code baseMask}
     * 相同的项是冗余的（用户行为本来就和组默认一致，无需特别列出）。因此白名单与黑名单永远不会同时存在 ——
     * 一个组要么是"默认可见 + 黑名单"({@code isDiscover=true})，要么是"默认不可见 + 白名单"({@code isDiscover=false})。
     * <p>
     * <strong>查询语义</strong>：用户 U 通过该组拿到 DISCOVER 当且仅当
     * {@code isDiscover XOR (U ∈ specifiedUsers)} 为真，等价于：
     * <pre>
     * (isDiscover=true  AND U NOT IN specifiedUsers)   // 默认放行，未被拉黑
     * OR
     * (isDiscover=false AND U IN     specifiedUsers)   // 默认拒绝，但被白名单
     * </pre>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComputedGroupAclProjection {

        /** 绑定组 ID（查询端用它做 ADMIN/OWNER 短路与成员组归属判断） */
        @Field(type = FieldType.Keyword)
        private String groupId;

        /** {@code baseMask & DISCOVER != 0}，即"该组普通成员能否默认看到这个资源" */
        @Field(type = FieldType.Boolean)
        private Boolean isDiscover;

        /**
         * 与 {@link #isDiscover} 行为<strong>相反</strong>的 userId 集合：
         * <ul>
         *   <li>{@code isDiscover=true } 时，本字段是<strong>黑名单</strong>（默认放行，列表内拒绝）；</li>
         *   <li>{@code isDiscover=false} 时，本字段是<strong>白名单</strong>（默认拒绝，列表内放行）。</li>
         * </ul>
         */
        @Field(type = FieldType.Keyword)
        @Builder.Default
        private List<String> specifiedUsers = new ArrayList<>();
    }
}
