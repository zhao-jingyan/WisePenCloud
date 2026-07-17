package com.oriole.wisepen.resource.domain;

import java.time.Instant;

public record InlineCommentCursor(Instant updatedAt, String threadId) {
}
