package com.oriole.wisepen.resource.domain;

import com.oriole.wisepen.resource.domain.base.MarketOfferOptionBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketOfferOption extends MarketOfferOptionBase {
    // 已售资源权限
    private Map<String, Integer> marketSpecifiedUsersGrantedActionsMask; // Market自行管理

    // 管理员审核
    private String auditorId;// 审核人 ID
    private String auditMessage;// 审核说明
    private LocalDateTime auditAt;// 审核完成时间
}
