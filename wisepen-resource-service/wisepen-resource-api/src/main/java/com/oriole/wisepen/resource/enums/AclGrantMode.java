package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 标签可见性模式枚举
 */
@Getter
@AllArgsConstructor
public enum AclGrantMode implements WisePenEnum {

    ALL(0, 0, "全部"),        // 全员下发配置的 Mask
    ONLY_ADMIN(1, 1, "仅管理员"), // 仅管理员下发 Mask（普通成员下发 0）
    WHITELIST(2, 2, "白名单"),  // 仅白名单内用户下发配置的 Mask
    BLACKLIST(3, 3, "黑名单");  // 仅黑名单外用户下发配置的 Mask

    @EnumValue
    private final Integer code;
    private final Integer value;
    private final String desc;
}
