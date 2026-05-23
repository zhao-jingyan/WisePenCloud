package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenPayerType implements WisePenEnum {
	USER(1, 1, "USER"),
	GROUP(2, 2, "GROUP");

	@EnumValue
	private final Integer code;

	private final Integer value;
	private final String desc;
}
