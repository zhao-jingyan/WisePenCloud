package com.oriole.wisepen.resource.domain.dto.req;

import com.oriole.wisepen.resource.constant.ResourceValidationMsg;
import com.oriole.wisepen.resource.enums.MarketPurchaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarketPurchaseRequest {
    @NotBlank(message = ResourceValidationMsg.RESOURCE_ID_NOT_BLANK)
    private String resourceId;

    @NotBlank(message = ResourceValidationMsg.GROUP_ID_NOT_BLANK)
    private String marketGroupId;

    @NotNull(message = ResourceValidationMsg.MARKET_PURCHASE_TYPE_NOT_NULL)
    private MarketPurchaseType purchaseType;
}
