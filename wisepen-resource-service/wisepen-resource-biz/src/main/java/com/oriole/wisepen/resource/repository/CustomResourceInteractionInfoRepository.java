package com.oriole.wisepen.resource.repository;

import com.oriole.wisepen.resource.domain.entity.ResourceInteractionInfoEntity;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 资源互动信息自定义 Repository，封装原子更新操作
 */
@Repository
public class CustomResourceInteractionInfoRepository {

    private final MongoTemplate mongoTemplate;

    public CustomResourceInteractionInfoRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 原子累加阅读量，upsert 兼容历史遗留资源（文档不存在时自动创建）
     */
    public void incrementReadCount(String resourceId, int delta) {
        Query query = Query.query(Criteria.where("_id").is(resourceId));
        Update update = new Update()
                .inc("readCount", delta)
                .setOnInsert("resourceId", resourceId);
        mongoTemplate.upsert(query, update, ResourceInteractionInfoEntity.class);
    }

    /**
     * 原子累加点赞数，upsert 兼容历史遗留资源（首次点赞时自动创建统计文档）
     */
    public void incrementLikeCount(String resourceId, int delta) {
        Query query = Query.query(Criteria.where("_id").is(resourceId));
        Update update = new Update()
                .inc("likeCount", delta)
                .setOnInsert("resourceId", resourceId);
        mongoTemplate.upsert(query, update, ResourceInteractionInfoEntity.class);
    }

    /**
     * 原子累加评分统计，upsert 保证文档不存在时自动创建
     * scoreAvg 为派生值，不存储，由 ResourceInteractInfoEntity.getScoreAvg() 在读取时计算。
     *
     * @param scoreCountDelta 首次评分传 1，覆盖评分传 0
     * @param scoreTotalDelta 分数增量
     */
    public void updateScoreStats(String resourceId, int scoreCountDelta, int scoreTotalDelta) {
        Query query = Query.query(Criteria.where("_id").is(resourceId));
        Update update = new Update()
                .inc("scoreCount", scoreCountDelta)
                .inc("scoreTotal", scoreTotalDelta)
                .setOnInsert("resourceId", resourceId);
        mongoTemplate.upsert(query, update, ResourceInteractionInfoEntity.class);
    }
}
