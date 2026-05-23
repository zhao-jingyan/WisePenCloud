package com.oriole.wisepen.user.api.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.oriole.wisepen.common.core.domain.enums.WisePenEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenTransactionType implements WisePenEnum {
	REFILL(1, 1, "REFILL"),
	SPEND(2, 2, "SPEND"),
	TRANSFER_IN(3, 3, "TRANSFER_IN"),
	TRANSFER_OUT(4, 4, "TRANSFER_OUT");

	@EnumValue
	private final Integer code;

	private final Integer value;
	private final String desc;
}
