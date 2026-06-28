package com.oriole.wisepen.note.api.domain.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteSnapshotResponse {
    private String resourceId;
    private Integer version;
    /** Base64 编码的最近一次 FULL 快照 */
    private String fullSnapshot;
    /** 最近 FULL 之后的 DELTA 增量链（Base64 数组），用于崩溃恢复 */
    private List<String> deltas;
}
