package com.oriole.wisepen.note.api.domain.base;

import com.oriole.wisepen.note.api.domain.enums.VersionType;
import lombok.Data;

@Data
public class NoteSnapshotBase {
    private String resourceId;
    private Integer version;
    private VersionType type;
    private String data;
    private String plainText;
}
