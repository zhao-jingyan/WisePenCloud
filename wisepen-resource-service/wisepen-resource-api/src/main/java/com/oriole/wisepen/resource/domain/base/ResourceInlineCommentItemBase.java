package com.oriole.wisepen.resource.domain.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceInlineCommentItemBase {
    private String itemId;
    private String authorId;
    private String content;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Builder.Default
    private List<String> mentionUserIds = new ArrayList<>();

    @Builder.Default
    private Map<String, ResourceInlineCommentItemReactionBase> reactions = new HashMap<>();

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
