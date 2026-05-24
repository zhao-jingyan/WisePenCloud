package com.oriole.wisepen.extension.fudan.domain.dto;

import com.oriole.wisepen.extension.fudan.enums.FudanUISTaskState;
import lombok.Data;
import java.util.Map;

@Data
public class FudanUISTaskResultDTO {
    private Integer state;        // 对应 UisTaskState 的 code
    private String message;       // 状态描述
    private String qrBase64;
    private Map<String, String> profile;

    // 静态工厂方法，方便快速构建
    public static FudanUISTaskResultDTO of(FudanUISTaskState state) {
        FudanUISTaskResultDTO dto = new FudanUISTaskResultDTO();
        dto.setState(state.getCode());
        dto.setMessage(state.getValue());
        return dto;
    }
}
