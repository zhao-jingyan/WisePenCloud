package com.oriole.wisepen.resource.domain.dto.req;

import com.oriole.wisepen.resource.constant.ResourceValidationMsg;
import com.oriole.wisepen.resource.enums.MarketOfferStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarketAuditOfferRequest {
    @NotBlank(message = ResourceValidationMsg.RESOURCE_ID_NOT_BLANK)
    private String resourceId;

    @NotBlank(message = ResourceValidationMsg.GROUP_ID_NOT_BLANK)
    private String marketGroupId;

    @NotNull(message = ResourceValidationMsg.MARKET_OFFER_STATUS_NOT_NULL)
    private MarketOfferStatus status;

    private String auditMessage;
}
