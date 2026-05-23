package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ModelType implements WisePenEnum {
	STANDARD_MODEL(1, 1, "STANDARD_MODEL"),
	ADVANCED_MODEL(2, 2, "ADVANCED_MODEL"),
	UNKNOWN_MODEL(3, 3, "UNKNOWN_MODEL");

	@EnumValue
	private final Integer code;
	private final Integer value;
	private final String desc;
}
