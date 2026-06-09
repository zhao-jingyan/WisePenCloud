package com.oriole.wisepen.system.api.domain.dto.req;

import com.oriole.wisepen.system.api.enums.FeedbackType;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Xiong Heng
 */
@Data
public class FeedbackRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String content;
    private String contact;
    private String browser;
    private FeedbackType type;
}