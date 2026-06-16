package com.oriole.wisepen.resource.domain.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oriole.wisepen.resource.constant.ResourceValidationMsg;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MarketPublishOfferRequest {
    @NotBlank(message = ResourceValidationMsg.RESOURCE_ID_NOT_BLANK)
    private String resourceId;

    @NotBlank(message = ResourceValidationMsg.GROUP_ID_NOT_BLANK)
    private String marketGroupId;

    @NotEmpty(message = ResourceValidationMsg.TAG_IDS_NOT_NULL)
    private List<String> tagIds;

    @Min(value = 1, message = ResourceValidationMsg.MARKET_PRICE_INVALID)
    private Integer forkOncePrice;

    @Min(value = 1, message = ResourceValidationMsg.MARKET_PRICE_INVALID)
    private Integer forkUnlimitedPrice;

    @NotNull(message = ResourceValidationMsg.MARKET_VERSION_NOT_NULL)
    @Min(value = 0, message = ResourceValidationMsg.MARKET_VERSION_INVALID)
    private Long offerVersion = 0L;

    @JsonIgnore
    @AssertTrue(message = ResourceValidationMsg.MARKET_OFFER_OPTION_REQUIRED)
    public boolean isOfferOptionSelected() {
        return forkOncePrice != null || forkUnlimitedPrice != null;
    }
}