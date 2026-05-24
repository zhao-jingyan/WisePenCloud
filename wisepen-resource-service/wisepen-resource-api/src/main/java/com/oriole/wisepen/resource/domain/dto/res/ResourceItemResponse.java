package com.oriole.wisepen.resource.domain.dto.res;

import com.oriole.wisepen.resource.domain.base.ResourceItemInfoBase;
import com.oriole.wisepen.resource.enums.ResourceAction;
import com.oriole.wisepen.user.api.domain.base.UserDisplayBase;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class ResourceItemResponse extends ResourceItemInfoBase {
    private String resourceId;
    private Integer readCount; // 聚合自资源互动信息表，缺失时返回 0
    private Integer likeCount; // 聚合自资源互动信息表，缺失时返回 0
    private Double scoreAvg;  // 聚合自资源互动信息表，无评分时为 null
    private Boolean liked;      // 当前用户点赞状态；无用户上下文时可为 null
    private Integer userScore;  // 当前用户评分；未评分时为 null
    private UserDisplayBase ownerInfo;

    private Map<String, String> currentTags;
    private List<ResourceAction> currentActions;

    private List<ResourceAction> overrideGrantedActions;
    private Map<String, List<ResourceAction>> specifiedUsersGrantedActions;
}
