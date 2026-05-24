package com.oriole.wisepen.resource.constant;

/**
 * 全文搜索模块常量。
 * <p>
 * 命名保留 Mongo 端的关键名词（ownerId / computedGroupAcls 等），并通过 {@code Discover}
 * 后缀显式标注"已投影到 DISCOVER 位"。
 */
public interface SearchConstants {

    // ============== 索引/分词器 ==============
    /** ES 索引名 */
    String RESOURCE_INDEX_NAME = "wisepen_resource_index";

    /** 建索引用：高召回，最大粒度切分 */
    String ANALYZER_IK_MAX_WORD = "ik_max_word";
    /** 检索用：高准确率，智能切分 */
    String ANALYZER_IK_SMART = "ik_smart";

    // ============== 检索相关 ==============
    /** 多字段加权打分配置（resourceName 最重要，tags 其次，content 兜底） */
    String[] BOOSTED_SEARCH_FIELDS = {"resourceName^3", "tags^2", "content"};

    /** 高亮前缀：与前端 wp-highlight 样式约定 */
    String HIGHLIGHT_PRE_TAG = "<em class=\"wp-highlight\">";
    String HIGHLIGHT_POST_TAG = "</em>";
    int HIGHLIGHT_FRAGMENT_SIZE = 100;
    int HIGHLIGHT_MAX_FRAGMENTS = 3;
    String HIGHLIGHT_FRAGMENT_SEPARATOR = "...";

    // ============== ES 字段名（建索引 + 查询双向引用） ==============
    String FIELD_RESOURCE_ID = "resourceId";
    String FIELD_RESOURCE_TYPE = "resourceType";
    String FIELD_RESOURCE_NAME = "resourceName";
    String FIELD_CONTENT = "content";
    String FIELD_TAGS = "tags";
    String FIELD_UPDATE_TIME = "updateTime";

    // ACL 投影字段（与 Mongo ResourceItemEntity 一一对应，但只投影 DISCOVER 位）
    String FIELD_OWNER_ID = "ownerId";
    String FIELD_SPECIFIED_DISCOVER_USERS = "specifiedDiscoverUsers";
    String FIELD_COMPUTED_GROUP_ACLS = "computedGroupAcls";

    // computedGroupAcls 嵌套子字段
    // 注意：白/黑名单已合并为 specifiedUsers（与 isDiscover 行为相反的"例外名单"），具体见 ESIndexEntity.ComputedGroupAclProjection。
    String FIELD_COMPUTED_GROUP_ACLS_GROUP_ID = "computedGroupAcls.groupId";
    String FIELD_COMPUTED_GROUP_ACLS_IS_DISCOVER = "computedGroupAcls.isDiscover";
    String FIELD_COMPUTED_GROUP_ACLS_SPECIFIED_USERS = "computedGroupAcls.specifiedUsers";

    // ============== ES 日期格式（兼容多种序列化形态） ==============
    /** 同时支持 ISO 标准 / 空格分隔 / epoch_millis 三种写入形态 */
    String ES_DATE_FORMAT_PATTERN =
            "yyyy-MM-dd'T'HH:mm:ss.SSS||yyyy-MM-dd'T'HH:mm:ss||yyyy-MM-dd HH:mm:ss||epoch_millis";

    // ============== 分页 ==============
    int MIN_PAGE_NUM = 1;
    int DEFAULT_PAGE_NUM = 1;
    int MIN_PAGE_SIZE = 1;
    int MAX_PAGE_SIZE = 100;
    int DEFAULT_PAGE_SIZE = 20;

    // ============== 线程池 ==============
    /** 搜索域专用调度线程池核心线程数（仅供延迟反查使用，与主链路隔离） */
    int COREPOOLSIZE = 5;

    // ============== Kafka 消费者组（搜索域内部约定，独立 groupId，与资源域不抢分区） ==============
    String CONSUMER_GROUP_DOC_READY = "wisepen-resource-search-doc-ready-group";
    String CONSUMER_GROUP_NOTE_SNAPSHOT = "wisepen-resource-search-note-snapshot-group";
    String CONSUMER_GROUP_ACL_RECALC = "wisepen-resource-es-acl-recalc-group";
    String CONSUMER_GROUP_PHYS_DESTROY = "wisepen-resource-search-physical-destroy-group";

    /** Kafka → ES 写入前的延迟（毫秒），用于规避资源域事务/Yjs 协同写入的竞态 */
    long ES_WRITE_DELAY_MS = 3000L;
}
