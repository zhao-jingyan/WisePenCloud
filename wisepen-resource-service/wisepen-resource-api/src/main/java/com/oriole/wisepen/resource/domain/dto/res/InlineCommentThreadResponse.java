package com.oriole.wisepen.resource.domain.dto.res;

import com.oriole.wisepen.resource.domain.base.InlineCommentAnchor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InlineCommentThreadResponse {
    private String threadId;
    private String resourceId;
    private InlineCommentAnchor anchor;
    private String quoteText;

    @Builder.Default
    private List<InlineCommentResponse> items = new ArrayList<>();

    private Long revision;
    private Long createdAt;
    private Long updatedAt;
}
