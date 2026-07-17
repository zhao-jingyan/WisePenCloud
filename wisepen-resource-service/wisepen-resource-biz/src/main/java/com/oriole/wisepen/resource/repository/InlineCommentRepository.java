package com.oriole.wisepen.resource.repository;

import com.oriole.wisepen.resource.domain.InlineCommentCursor;
import com.oriole.wisepen.resource.domain.entity.InlineCommentThreadEntity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InlineCommentRepository {

    private final MongoTemplate mongoTemplate;

    @PostConstruct
    public void ensureIndexes() {
        mongoTemplate.indexOps(InlineCommentThreadEntity.class).ensureIndex(
                new Index()
                        .named("uniq_inline_comment_create_idempotency")
                        .on("resourceId", Direction.ASC)
                        .on("createIdempotencyKey", Direction.ASC)
                        .unique()
        );
        mongoTemplate.indexOps(InlineCommentThreadEntity.class).ensureIndex(
                new Index()
                        .named("idx_inline_comment_changes")
                        .on("resourceId", Direction.ASC)
                        .on("updatedAt", Direction.ASC)
                        .on("_id", Direction.ASC)
        );
    }

    public InlineCommentThreadEntity insert(InlineCommentThreadEntity thread) {
        return mongoTemplate.insert(thread);
    }

    public Optional<InlineCommentThreadEntity> findById(String threadId) {
        return Optional.ofNullable(mongoTemplate.findById(threadId, InlineCommentThreadEntity.class));
    }

    public Optional<InlineCommentThreadEntity> findByResourceIdAndCreateIdempotencyKey(
            String resourceId,
            String idempotencyKey
    ) {
        Query query = Query.query(Criteria.where("resourceId").is(resourceId)
                .and("createIdempotencyKey").is(idempotencyKey));
        return Optional.ofNullable(mongoTemplate.findOne(query, InlineCommentThreadEntity.class));
    }

    public List<InlineCommentThreadEntity> findAllByResourceId(String resourceId) {
        Query query = Query.query(Criteria.where("resourceId").is(resourceId))
                .with(Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.desc("_id")
                ));
        return mongoTemplate.find(query, InlineCommentThreadEntity.class);
    }

    public Optional<InlineCommentThreadEntity> appendInlineComment(
            String threadId,
            long expectedRevision,
            InlineCommentThreadEntity.Item comment,
            Instant updatedAt
    ) {
        Query query = Query.query(Criteria.where("_id").is(threadId)
                .and("revision").is(expectedRevision)
                .and("items.idempotencyKey").ne(comment.getIdempotencyKey()));
        Update update = new Update()
                .push("items", comment)
                .inc("revision", 1L)
                .set("updatedAt", updatedAt);
        InlineCommentThreadEntity updated = mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                InlineCommentThreadEntity.class
        );
        return Optional.ofNullable(updated);
    }

    public List<InlineCommentThreadEntity> findChanges(
            String resourceId,
            InlineCommentCursor cursor,
            int limit
    ) {
        Criteria cursorBoundary = new Criteria().orOperator(
                Criteria.where("updatedAt").gt(cursor.updatedAt()),
                new Criteria().andOperator(
                        Criteria.where("updatedAt").is(cursor.updatedAt()),
                        Criteria.where("_id").gt(cursor.threadId())
                )
        );
        Query query = Query.query(new Criteria().andOperator(
                        Criteria.where("resourceId").is(resourceId),
                        cursorBoundary
                ))
                .with(Sort.by(
                        Sort.Order.asc("updatedAt"),
                        Sort.Order.asc("_id")
                ))
                .limit(limit);
        return mongoTemplate.find(query, InlineCommentThreadEntity.class);
    }
}
