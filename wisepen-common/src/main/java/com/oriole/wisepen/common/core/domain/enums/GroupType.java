package com.oriole.wisepen.common.core.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum GroupType implements WisePenEnum {
	NORMAL_GROUP(1, 1, "NORMAL_GROUP"),
	ADVANCED_GROUP(2, 2, "ADVANCED_GROUP"),
	MARKET_GROUP(3, 3, "MARKET_GROUP");


	@EnumValue
	private final Integer code;

	private final Integer value;
	private final String desc;

	public static GroupType getByCode(Integer code) {
		if (code == null) {return null;}
		return Arrays.stream(values())
				.filter(t -> t.getCode().equals(code))
				.findFirst()
				.orElse(null);
	}
}
