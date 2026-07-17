package com.oriole.wisepen.resource.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wisepen_resource_inline_comment_threads")
public class InlineCommentThreadEntity {

    @Id
    private String threadId;
    private String resourceId;
    private String createIdempotencyKey;
    private Anchor anchor;
    private String quoteText;
    private Long revision;

    @Builder.Default
    private List<Item> items = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Anchor {
        private String start;
        private String end;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String commentId;
        private String idempotencyKey;
        private String authorId;
        private Author author;
        private String content;
        private Instant createdAt;
        private Long revision;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Author {
        private String id;
        private String name;
        private String avatar;
    }
}
