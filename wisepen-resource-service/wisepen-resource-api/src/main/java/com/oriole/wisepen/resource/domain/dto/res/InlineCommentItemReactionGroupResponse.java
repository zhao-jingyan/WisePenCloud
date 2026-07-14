package com.oriole.wisepen.resource.domain.dto.res;

import com.oriole.wisepen.user.api.domain.base.UserDisplayBase;
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
public class InlineCommentItemReactionGroupResponse {
    private String emojiId;

    @Builder.Default
    private Integer count = 0;

    @Builder.Default
    private Boolean reactedByCurrentUser = false;

    @Builder.Default
    private List<UserDisplayBase> users = new ArrayList<>();
}
