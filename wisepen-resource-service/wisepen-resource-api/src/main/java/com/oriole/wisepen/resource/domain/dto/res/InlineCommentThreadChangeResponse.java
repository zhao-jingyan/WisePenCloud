package com.oriole.wisepen.resource.domain.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InlineCommentThreadChangeResponse {
    private String threadId;
    private Long revision;
}
