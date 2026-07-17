package com.oriole.wisepen.resource.domain.dto.req;

import com.oriole.wisepen.resource.domain.base.InlineCommentAnchor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InlineCommentThreadCreateRequest {

    @NotBlank
    private String resourceId;

    @NotBlank
    private String idempotencyKey;

    @Valid
    @NotNull
    private InlineCommentAnchor anchor;

    @NotBlank
    private String quoteText;

    @NotBlank
    private String content;
}
