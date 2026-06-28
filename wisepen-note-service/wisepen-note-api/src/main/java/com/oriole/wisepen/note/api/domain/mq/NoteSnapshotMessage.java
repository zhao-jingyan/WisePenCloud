package com.oriole.wisepen.note.api.domain.mq;

import com.oriole.wisepen.note.api.domain.base.NoteSnapshotBase;
import lombok.*;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteSnapshotMessage extends NoteSnapshotBase {
    private List<String> updatedBy;
}
