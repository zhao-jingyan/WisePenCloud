package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VoucherStatus implements WisePenEnum {
	UNUSED(1, 1, "UNUSED"),
	USED(2, 2, "USED"),
	DISABLED(3, 3, "DISABLED");

	@EnumValue
	private final Integer code;

	private final Integer value;
	private final String desc;
}
