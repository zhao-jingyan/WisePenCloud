package com.oriole.wisepen.resource.repository;

import com.oriole.wisepen.resource.domain.entity.ESIndexEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * ES 索引文档的 CRUD 兜底（主要是 {@code deleteById}）。
 * <p>
 * 复杂查询（多字段加权 + ACL 4-case + 高亮）直接由 {@code SearchQueryServiceImpl}
 * 通过 {@code ElasticsearchOperations} 完成，不走 Repository。
 */
@Repository
public interface ESIndexRepository extends ElasticsearchRepository<ESIndexEntity, String> {
}
