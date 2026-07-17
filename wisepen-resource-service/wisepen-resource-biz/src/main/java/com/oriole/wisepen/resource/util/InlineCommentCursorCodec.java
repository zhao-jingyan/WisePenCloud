package com.oriole.wisepen.resource.util;

import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.resource.domain.InlineCommentCursor;
import com.oriole.wisepen.resource.exception.ResourceError;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class InlineCommentCursorCodec {

    private static final int HEADER_BYTES = Long.BYTES + Integer.BYTES;
    private static final int MAX_THREAD_ID_BYTES = 256;

    public String encode(InlineCommentCursor cursor) {
        byte[] threadIdBytes = cursor.threadId().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_BYTES + threadIdBytes.length);
        buffer.putLong(cursor.updatedAt().toEpochMilli());
        buffer.putInt(threadIdBytes.length);
        buffer.put(threadIdBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    }

    public InlineCommentCursor decode(String encoded) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(encoded);
            if (bytes.length < HEADER_BYTES) {
                throw new IllegalArgumentException("cursor header missing");
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            long updatedAt = buffer.getLong();
            int threadIdLength = buffer.getInt();
            if (threadIdLength < 0 || threadIdLength > MAX_THREAD_ID_BYTES || buffer.remaining() != threadIdLength) {
                throw new IllegalArgumentException("cursor thread id invalid");
            }
            byte[] threadIdBytes = new byte[threadIdLength];
            buffer.get(threadIdBytes);
            return new InlineCommentCursor(
                    Instant.ofEpochMilli(updatedAt),
                    new String(threadIdBytes, StandardCharsets.UTF_8)
            );
        } catch (RuntimeException exception) {
            throw new ServiceException(ResourceError.INLINE_COMMENT_CURSOR_INVALID);
        }
    }
}
