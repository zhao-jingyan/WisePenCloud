package com.oriole.wisepen.ai.asset.domain.dto.res;

import com.oriole.wisepen.ai.asset.domain.base.SkillInfoBase;
import com.oriole.wisepen.ai.asset.domain.base.SkillVersionBase;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LatestPublishedSkillInfoResponse extends SkillInfoBase {
    private String resourceId;
    private SkillVersionBase latestPublishedSkill;
}
