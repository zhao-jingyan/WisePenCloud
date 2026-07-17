package com.oriole.wisepen.resource.domain.base;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InlineCommentAnchor {

    @NotBlank
    private String start;

    @NotBlank
    private String end;
}
