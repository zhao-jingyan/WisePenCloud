package com.oriole.wisepen.resource.domain.base;

import com.oriole.wisepen.resource.enums.MarketOfferStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MarketOfferOptionBase {
    // 预览配置
    private Integer reviewActionsMask; // 未购买资源的用户可以获得的权限掩码
    // 预览配置
    private int reviewContentPercentage; // 可预览内容百分比

    // 售卖信息
    private List<MarketOfferInfoBase> marketOfferList;

    // 售卖状态
    private MarketOfferStatus status = MarketOfferStatus.PENDING; // 状态
    private Integer offerVersion; // 指定售卖版本
}
