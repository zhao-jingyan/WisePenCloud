package com.oriole.wisepen.system.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户反馈类型枚举
 * @author Xiong.Heng
 */
@Getter
@AllArgsConstructor
public enum FeedbackType implements WisePenEnum {
    BUG_REPORT(1, 1, "BUG_REPORT"),
    SUGGESTION(2, 2, "SUGGESTION"),
    CONSULTATION(3, 3, "CONSULTATION"),
    COMPLAINT(4, 4, "COMPLAINT"),
    // 无法归类到以上几类的反馈
    OTHER(99, 99, "其他");

    @EnumValue
    private final Integer code;
    private final Integer value;
    private final String desc;

}
