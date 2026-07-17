package com.oriole.wisepen.resource.domain.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InlineCommentCreateRequest {

    @NotBlank
    private String idempotencyKey;

    @NotBlank
    private String content;
}
