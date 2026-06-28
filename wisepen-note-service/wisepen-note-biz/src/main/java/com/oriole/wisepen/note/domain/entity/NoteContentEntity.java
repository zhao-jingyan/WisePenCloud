package com.oriole.wisepen.note.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "note_content")
public class NoteContentEntity {
    @Id
    private String resourceId;

    private Integer version;

    private String rawText;

    @CreatedDate
    private LocalDateTime createTime;
}
