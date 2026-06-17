package com.oriole.wisepen.resource.domain.base;

import cn.hutool.core.util.IdUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOfferInfoBase {
    private String offerId = IdUtil.fastSimpleUUID();
    // 售卖详情
    private Integer grantedActionsMask; // 购买资源的用户可以获得的权限掩码
    private Integer price; // 售卖价格
    private LocalDateTime createAt;
}
