package com.oriole.wisepen.resource.service;

import com.oriole.wisepen.resource.domain.entity.ESIndexEntity;
import com.oriole.wisepen.resource.enums.UpsertField;

/**
 * 搜索引擎写入路径的唯一入口。
 * <p>
 * 所有 ES 写操作（4 个 Kafka 消费者 + 后台初始化任务）都汇入这里，统一走 Upsert
 * （ES 端 {@code docAsUpsert=true}），按 {@link UpsertField} 控制粒度避免覆盖。
 * <p>
 * 每次 Upsert 都会强制写入 {@code resourceId / resourceType / updateTime} 这三个"基础不变量"，
 * 防止 Upsert 产生的新文档缺关键字段。
 */
public interface ISearchSyncService {

    /** 仅写 {@code ACL + TAGS + RESOURCE_NAME}，给 ACL 重算/标签变更使用 */
    void upsertIndexMetaData(ESIndexEntity entity);

    /** 仅写 {@code RESOURCE_NAME + CONTENT}，给 Note 快照/Document 文档内容更新使用 */
    void upsertIndexContent(ESIndexEntity entity);

    /** 写入全部字段，给首次入库/索引重建使用 */
    void upsertFullIndex(ESIndexEntity entity);

    /** 物理删除：按 {@code esId}（即 {@code <resourceType>_<resourceId>}）幂等删除 */
    void deleteIndex(String esId);
}
