package com.oriole.wisepen.resource.domain.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 资源互动信息基类，承载阅读量等可持续扩展的互动统计字段
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceInteractionInfoBase {
    private Integer readCount = 0;   // 资源有效阅读量，默认 0
    private Integer likeCount = 0;   // 点赞总数，默认 0
    private Integer scoreCount = 0;  // 评分人数，默认 0
    private Integer scoreTotal = 0;  // 评分总和，默认 0
}
