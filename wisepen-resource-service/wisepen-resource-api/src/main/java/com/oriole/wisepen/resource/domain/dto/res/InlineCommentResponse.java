package com.oriole.wisepen.resource.domain.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InlineCommentResponse {
    private String commentId;
    private String authorId;
    private InlineCommentAuthorResponse author;
    private String content;
    private Long createdAt;
    private Long revision;
}
