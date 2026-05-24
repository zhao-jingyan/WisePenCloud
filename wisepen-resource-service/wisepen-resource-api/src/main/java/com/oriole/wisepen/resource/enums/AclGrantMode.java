package com.oriole.wisepen.resource.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AclGrantMode {

    ALL(0, "ALL"),                // 全员下发配置的 Mask
    ONLY_ADMIN(1, "ONLY_ADMIN"),  // 仅管理员下发 Mask（普通成员下发 0）
    WHITELIST(2, "WHITELIST"),    // 仅白名单内用户下发配置的 Mask
    BLACKLIST(3, "BLACKLIST");    // 仅黑名单外用户下发配置的 Mask

    @EnumValue
    @JsonValue
    private final int code;

    private final String value;
}
