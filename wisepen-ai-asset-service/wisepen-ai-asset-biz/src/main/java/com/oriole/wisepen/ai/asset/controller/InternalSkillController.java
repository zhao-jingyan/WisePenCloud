package com.oriole.wisepen.ai.asset.controller;

import cn.hutool.core.bean.BeanUtil;
import com.oriole.wisepen.ai.asset.domain.base.SkillInfoBase;
import com.oriole.wisepen.ai.asset.domain.dto.res.LatestPublishedSkillInfoResponse;
import com.oriole.wisepen.ai.asset.service.ISkillService;
import com.oriole.wisepen.ai.asset.service.ISkillVersionService;
import com.oriole.wisepen.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/skill")
@RequiredArgsConstructor
public class InternalSkillController {

    private final ISkillService skillService;
    private final ISkillVersionService skillVersionService;

    @GetMapping("/getPublishedSkillByResourceId")
    public R<LatestPublishedSkillInfoResponse> getPublishedSkillByResourceId(@RequestParam String resourceId) {
        SkillInfoBase skill = skillService.getSkillInfo(resourceId);
        LatestPublishedSkillInfoResponse response = BeanUtil.copyProperties(skill, LatestPublishedSkillInfoResponse.class);
        response.setLatestPublishedSkill(skillVersionService.getSkillVersion(resourceId, skill.getVersion()));
        return R.ok(response);
    }
}
