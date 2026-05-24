package com.oriole.wisepen.resource.repository;

import com.oriole.wisepen.resource.domain.entity.ResourceUserInteractRecordEntity;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * 用户资源互动记录自定义 Repository，封装需要原子读写语义的操作。
 */
@Repository
public class CustomResourceUserInteractRecordRepository {

    private final MongoTemplate mongoTemplate;

    public CustomResourceUserInteractRecordRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 原子写入点赞状态（upsert：首次操作自动创建记录）。
     */
    public ResourceUserInteractRecordEntity findAndSetLiked(String resourceId, String userId, boolean liked) {
        Query query = Query.query(
                Criteria.where("resourceId").is(resourceId).and("userId").is(userId));
        Update update = new Update()
                .set("liked", liked)
                .setOnInsert("resourceId", resourceId)
                .setOnInsert("userId", userId);
        return mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().upsert(true).returnNew(false),
                ResourceUserInteractRecordEntity.class);
    }

    /**
     * 原子写入评分，返回写入前的文档。
     * upsert=true：首次评分自动创建记录；returnNew=false：返回旧状态供调用方判断首次/覆盖并计算统计增量。
     */
    public ResourceUserInteractRecordEntity findAndSetScore(String resourceId, String userId, int score) {
        Query query = Query.query(
                Criteria.where("resourceId").is(resourceId).and("userId").is(userId));
        Update update = new Update()
                .set("score", score)
                .setOnInsert("resourceId", resourceId)
                .setOnInsert("userId", userId);
        return mongoTemplate.findAndModify(
                query, update,
                FindAndModifyOptions.options().upsert(true).returnNew(false),
                ResourceUserInteractRecordEntity.class);
    }
}
