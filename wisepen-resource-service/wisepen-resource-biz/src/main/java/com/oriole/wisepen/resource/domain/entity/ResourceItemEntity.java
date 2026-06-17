package com.oriole.wisepen.resource.domain.entity;

import com.oriole.wisepen.resource.domain.ComputedGroupAcl;
import com.oriole.wisepen.resource.domain.GroupTagBind;
import com.oriole.wisepen.resource.domain.base.ResourceItemInfoBase;
import com.oriole.wisepen.resource.enums.MarketOfferStatus;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@Document(collection = "wisepen_resource_items")
public class ResourceItemEntity extends ResourceItemInfoBase {
    @Id
    private String resourceId; // 资源全局唯一ID

    private List<GroupTagBind> groupBinds = new ArrayList<>();

    // 预计算后的运行时权限
    private Map<String, ComputedGroupAcl> computedGroupAcls;

    /** 资源级组权限覆盖：若用户组命中此 Map，将无视 computedGroupAcls 的值
     *  key 为用户组 id， Integer 为 resourceActionsMask */
    private Map<String, Integer> overrideGrantedActionsMask;

    /** 资源级用户权限：若用户命中此 Map，直接返回该值，无视其他所有规则
     *  key 为用户 id， Integer 为 resourceActionsMask */
    private Map<String, Integer> specifiedUsersGrantedActionsMask;

    @CreatedDate
    private LocalDateTime createTime;
    @LastModifiedDate
    private LocalDateTime updateTime;

    /** 资源删除时间，非 null 表示已删除；定时任务据此判断是否到期硬删*/
    private LocalDateTime deletedAt;

    public void offShelfMarketOffer(String groupID) {
        // 如果绑定了 Market 组
        groupBinds.stream().filter(groupTagBind->groupTagBind.getGroupId().equals(groupID)).findFirst().ifPresent(groupTagBind -> {
            // 设置该 Market 组权限掩码为 0
            overrideGrantedActionsMask.put(groupID, 0);
            // 该 Market 组 Offer 状态不是 BANNED 时，转为 OFF_SHELF
            if (groupTagBind.getMarketOffer().getStatus() != MarketOfferStatus.BANNED) {
                groupTagBind.getMarketOffer().setStatus(MarketOfferStatus.OFF_SHELF);
            }
        });
    }
}