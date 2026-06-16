package com.oriole.wisepen.resource.domain.dto.res;

import com.oriole.wisepen.resource.domain.base.MarketOrderBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class MarketOrderResponse extends MarketOrderBase {
    private String orderId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
