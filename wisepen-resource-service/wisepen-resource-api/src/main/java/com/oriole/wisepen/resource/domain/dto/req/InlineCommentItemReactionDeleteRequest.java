package com.oriole.wisepen.resource.domain.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InlineCommentItemReactionDeleteRequest {
    @NotBlank
    private String resourceId;

    @NotBlank
    private String inlineCommentId;

    @NotBlank
    private String itemId;

    private Integer contentVersion;
}
