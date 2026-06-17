package com.oriole.wisepen.resource.domain.dto.res;

import com.oriole.wisepen.resource.enums.MarketOfferStatus;
import com.oriole.wisepen.resource.enums.ResourceAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketOfferOptionResponse {
    // 预览配置
    private List<ResourceAction> reviewActions; // 未购买资源的用户可以获得的权限
    // 预览配置
    private int reviewContentPercentage; // 可预览内容百分比

    // 售卖信息
    private List<MarketOfferInfoResponse> marketOfferList;

    // 售卖状态
    private MarketOfferStatus status; // 状态
    private Integer offerVersion; // 指定售卖版本


    private String auditMessage;// 审核说明
    private LocalDateTime auditAt;// 审核完成时间
}
