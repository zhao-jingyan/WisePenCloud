package com.oriole.wisepen.resource.service.impl;

import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.resource.constant.SearchConstants;
import com.oriole.wisepen.resource.domain.entity.ESIndexEntity;
import com.oriole.wisepen.resource.enums.UpsertField;
import com.oriole.wisepen.resource.exception.SearchErrorCode;
import com.oriole.wisepen.resource.repository.ESIndexRepository;
import com.oriole.wisepen.resource.service.ISearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 搜索同步实现：所有写操作汇入此处，按 {@link UpsertField} 选择性写字段。
 * <p>
 * 走 ES 的 Update API，参数 {@code doc_as_upsert=true}：文档存在 → partial update；文档不存在 → 用本次 doc 创建。
 * 因此每次都会强制带上 {@code resourceId / resourceType / updateTime} 三个不变量，
 * 防止 Upsert 创建出来的新文档缺关键字段。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchSyncServiceImpl implements ISearchSyncService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ESIndexRepository esIndexRepository;

    private static final IndexCoordinates INDEX = IndexCoordinates.of(SearchConstants.RESOURCE_INDEX_NAME);

    @Override
    public void upsertIndexMetaData(ESIndexEntity entity) {
        executeUpsert(entity, EnumSet.of(UpsertField.ACL, UpsertField.TAGS, UpsertField.RESOURCE_NAME));
    }

    @Override
    public void upsertIndexContent(ESIndexEntity entity) {
        executeUpsert(entity, EnumSet.of(UpsertField.RESOURCE_NAME, UpsertField.CONTENT));
    }

    @Override
    public void upsertFullIndex(ESIndexEntity entity) {
        executeUpsert(entity, EnumSet.allOf(UpsertField.class));
    }

    /**
     * 收敛三个公开 upsert 方法的共用代码：构 partial doc → 走 ES Update API（{@code doc_as_upsert=true}）。
     * 实现细节，不暴露在 {@link ISearchSyncService} 接口上。
     */
    private void executeUpsert(ESIndexEntity entity, EnumSet<UpsertField> fields) {
        if (entity == null || entity.getId() == null) {
            log.warn("ES upsert skipped: entity or id is null");
            return;
        }
        if (fields == null || fields.isEmpty()) {
            log.warn("ES upsert skipped: no fields chosen esId={}", entity.getId());
            return;
        }
        ensureIndexExists();

        try {
            Map<String, Object> partialDoc = buildPartialDoc(entity, fields);

            // Spring Data ES 8 中 UpdateQuery.withDocument(...) 接受
            // org.springframework.data.elasticsearch.core.document.Document，需把 Map 包一层。
            UpdateQuery updateQuery = UpdateQuery.builder(entity.getId())
                    .withDocument(Document.from(partialDoc))
                    .withDocAsUpsert(true)
                    .build();

            elasticsearchOperations.update(updateQuery, INDEX);
            log.debug("ES upsert ok esId={} fields={}", entity.getId(), fields);
        } catch (Exception e) {
            log.error("ES upsert failed esId={} fields={}", entity.getId(), fields, e);
            throw new ServiceException(SearchErrorCode.ES_CONNECTION_ERROR);
        }
    }

    @Override
    public void deleteIndex(String esId) {
        if (esId == null || esId.isBlank()) {
            return;
        }
        try {
            esIndexRepository.deleteById(esId);
            log.info("ES doc deleted esId={}", esId);
        } catch (Exception e) {
            // 删除幂等，找不到也不算错；其他错误打 warn 后吞掉，由上游决定重试
            log.warn("ES delete failed esId={}", esId, e);
        }
    }

    // ============== 内部 ==============

    /**
     * 按 {@link UpsertField} 集合拼一份 partial doc。
     * <p>
     * {@code resourceId / resourceType / updateTime} 三个字段始终带上（Upsert 不变量）。
     */
    private Map<String, Object> buildPartialDoc(ESIndexEntity entity, EnumSet<UpsertField> fields) {
        Map<String, Object> doc = new HashMap<>();

        // 不变量：每次都写
        doc.put(SearchConstants.FIELD_RESOURCE_ID, entity.getResourceId());
        doc.put(SearchConstants.FIELD_RESOURCE_TYPE, entity.getResourceType());
        doc.put(SearchConstants.FIELD_UPDATE_TIME,
                entity.getUpdateTime() == null ? LocalDateTime.now() : entity.getUpdateTime());

        if (fields.contains(UpsertField.RESOURCE_NAME) && entity.getResourceName() != null) {
            doc.put(SearchConstants.FIELD_RESOURCE_NAME, entity.getResourceName());
        }
        if (fields.contains(UpsertField.CONTENT) && entity.getContent() != null) {
            doc.put(SearchConstants.FIELD_CONTENT, entity.getContent());
        }
        if (fields.contains(UpsertField.TAGS)) {
            doc.put(SearchConstants.FIELD_TAGS, entity.getTags());
        }
        if (fields.contains(UpsertField.ACL)) {
            doc.put(SearchConstants.FIELD_OWNER_ID, entity.getOwnerId());
            doc.put(SearchConstants.FIELD_SPECIFIED_DISCOVER_USERS, entity.getSpecifiedDiscoverUsers());
            doc.put(SearchConstants.FIELD_COMPUTED_GROUP_ACLS, entity.getComputedGroupAcls());
        }
        return doc;
    }

    /** 确保索引存在（mapping 由 {@code @Document(createIndex=true)} 在第一次访问时建立） */
    private void ensureIndexExists() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(ESIndexEntity.class);
            if (!indexOps.exists()) {
                indexOps.create();
                indexOps.putMapping();
                log.info("ES index created indexName={}", SearchConstants.RESOURCE_INDEX_NAME);
            }
        } catch (Exception e) {
            // 不阻断主路径：即使建索引出错（如并发竞争），update API 也会回报真实失败
            log.warn("ES index ensure failed indexName={}", SearchConstants.RESOURCE_INDEX_NAME, e);
        }
    }
}
