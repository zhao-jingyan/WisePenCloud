package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModelType {
	STANDARD_MODEL(1,"STANDARD_MODEL"),
	ADVANCED_MODEL(2,"ADVANCED_MODEL"),
	UNKNOWN_MODEL(3,"UNKNOWN_MODEL");

	@EnumValue
	@JsonValue
	private final int code;
	private final String value;
}
