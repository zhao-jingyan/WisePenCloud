package com.oriole.wisepen.extension.fudan.controller;

import com.oriole.wisepen.common.core.domain.R;
import com.oriole.wisepen.common.core.exception.ServiceException;
import com.oriole.wisepen.extension.fudan.cache.RedisCacheManager;
import com.oriole.wisepen.extension.fudan.domain.dto.FudanUISTaskResultDTO;
import com.oriole.wisepen.extension.fudan.exception.FudanExtensionError;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/extenion/fudan/uis")
@RequiredArgsConstructor
@Hidden
public class UISTaskController {

    private final RedisCacheManager redisCacheManager;

    @GetMapping("/getUISVerificationStatus")
    public R<FudanUISTaskResultDTO> getTaskStatus(@RequestParam Long userId) {
        FudanUISTaskResultDTO result = redisCacheManager.getUisTaskStatus(userId);

        if (result == null) {
            throw new ServiceException(FudanExtensionError.UIS_TASK_NOT_FOUND);
        }
        return R.ok(result);
    }
}
