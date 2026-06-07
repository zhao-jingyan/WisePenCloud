package com.oriole.wisepen.resource.service.impl;

import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.resource.constant.SearchConstants;
import com.oriole.wisepen.resource.domain.entity.ESIndexEntity;
import com.oriole.wisepen.resource.domain.entity.ResourceItemEntity;
import com.oriole.wisepen.resource.enums.UpsertField;
import com.oriole.wisepen.resource.exception.ResourceError;
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

import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchSyncServiceImpl implements ISearchSyncService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ESIndexRepository esIndexRepository;

    private static final IndexCoordinates INDEX = IndexCoordinates.of(SearchConstants.RESOURCE_INDEX_NAME);

    @Override
    public void syncResourceMetadata(ResourceItemEntity entity, EnumSet<UpsertField> fields) {
        if (entity == null || entity.getResourceId() == null) return;
        executeUpsert(new ESIndexEntity(entity), fields);
    }

    @Override
    public void syncResourceContent(String resourceId, String content) {
        if (resourceId == null) return;
        executeUpsert(new ESIndexEntity(resourceId, content), EnumSet.of(UpsertField.CONTENT));
    }

    @Override
    public void deleteResourceIndex(String resourceId) {
        try {
            esIndexRepository.deleteById(resourceId);
            log.info("search document deleted. resourceId={}", resourceId);
        } catch (Exception e) {
            log.warn("search document delete failed. resourceId={}", resourceId, e);
        }
    }

    private void executeUpsert(ESIndexEntity entity, EnumSet<UpsertField> fields) {
        // 确保索引库存在
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(ESIndexEntity.class);
            if (!indexOps.exists()) {
                indexOps.create();
                indexOps.putMapping();
                log.info("search index created. indexName={}", SearchConstants.RESOURCE_INDEX_NAME);
            }
        } catch (Exception e) {
            log.warn("search index ensure failed. indexName={}", SearchConstants.RESOURCE_INDEX_NAME, e);
        }

        try {
            Map<String, Object> doc = new HashMap<>();
            doc.put("resourceId", entity.getResourceId());
            doc.put("updateTime", entity.getUpdateTime().truncatedTo(ChronoUnit.MILLIS)
                    .format(DateTimeFormatter.ofPattern(SearchConstants.ES_DATE_FORMAT_PATTERN)));

            if (fields.contains(UpsertField.RESOURCE_TYPE)) {
                doc.put("resourceType", entity.getResourceType());
            }

            if (fields.contains(UpsertField.RESOURCE_NAME) && entity.getResourceName() != null) {
                doc.put("resourceName", entity.getResourceName());
            }
            if (fields.contains(UpsertField.CONTENT) && entity.getContent() != null) {
                doc.put("content", entity.getContent());
            }
            if (fields.contains(UpsertField.ACL)) {
                doc.put("ownerId", entity.getOwnerId());
                doc.put("specifiedDiscoverUsers", entity.getSpecifiedDiscoverUsers());
                doc.put("computedGroupAcls", entity.getComputedGroupAcls());
            }

            UpdateQuery updateQuery = UpdateQuery.builder(entity.getResourceId())
                    .withDocument(Document.from(doc))
                    .withDocAsUpsert(true)
                    .build();

            elasticsearchOperations.update(updateQuery, INDEX);
            log.debug("search document upsert succeeded. resourceId={} fields={}", entity.getResourceId(), fields);
        } catch (Exception e) {
            log.error("search document upsert failed. resourceId={} fields={}", entity.getResourceId(), fields, e);
            throw new ServiceException(ResourceError.RESOURCE_SEARCH_FAILED);
        }
    }
}
