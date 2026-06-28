package com.oriole.wisepen.note.api.domain.base;

import com.oriole.wisepen.note.api.domain.enums.VersionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NoteVersionBase {
    private VersionType type;
    private List<Long> createdBy;
}
