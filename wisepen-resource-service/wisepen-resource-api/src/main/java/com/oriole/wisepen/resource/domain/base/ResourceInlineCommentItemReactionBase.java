package com.oriole.wisepen.resource.domain.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceInlineCommentItemReactionBase {
    private String emojiId;
    private LocalDateTime createTime;
}
