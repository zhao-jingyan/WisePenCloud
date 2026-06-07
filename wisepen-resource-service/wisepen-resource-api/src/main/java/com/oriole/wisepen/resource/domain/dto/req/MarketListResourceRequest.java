package com.oriole.wisepen.resource.domain.dto.req;

import com.oriole.wisepen.resource.constant.ResourceValidationMsg;
import com.oriole.wisepen.resource.enums.MarketSellMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MarketListResourceRequest {
    @NotBlank(message = ResourceValidationMsg.RESOURCE_ID_NOT_BLANK)
    private String resourceId;

    @NotBlank(message = ResourceValidationMsg.GROUP_ID_NOT_BLANK)
    private String marketGroupId;

    @NotEmpty(message = ResourceValidationMsg.TAG_IDS_NOT_NULL)
    private List<String> tagIds;

    @NotNull(message = ResourceValidationMsg.MARKET_PRICE_NOT_NULL)
    @Min(value = 1, message = ResourceValidationMsg.MARKET_PRICE_INVALID)
    private Integer price;

    @NotNull(message = ResourceValidationMsg.MARKET_SELL_METHOD_NOT_NULL)
    private MarketSellMethod sellMethod;

    @Min(value = 0, message = ResourceValidationMsg.MARKET_VERSION_INVALID)
    private Long listedVersion = 0L;
}
