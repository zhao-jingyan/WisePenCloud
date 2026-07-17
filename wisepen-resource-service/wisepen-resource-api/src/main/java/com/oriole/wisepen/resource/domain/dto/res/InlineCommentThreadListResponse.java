package com.oriole.wisepen.resource.domain.dto.res;

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
public class InlineCommentThreadListResponse {

    @Builder.Default
    private List<InlineCommentThreadResponse> items = new ArrayList<>();

    private String cursor;
}
