package com.oriole.wisepen.resource.domain.dto.req;

import com.oriole.wisepen.resource.constant.ResourceValidationMsg;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResourceRateRequest {
    @NotBlank(message = ResourceValidationMsg.RESOURCE_ID_NOT_BLANK)
    private String resourceId;

    @NotNull(message = ResourceValidationMsg.SCORE_RANGE_INVALID)
    @Min(value = 1, message = ResourceValidationMsg.SCORE_RANGE_INVALID)
    @Max(value = 5, message = ResourceValidationMsg.SCORE_RANGE_INVALID)
    private Integer score;
}
