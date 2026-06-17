package com.oriole.wisepen.resource.domain.mq;

import com.oriole.wisepen.resource.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceForkMessage {
    private String forkTaskId;

    private String sourceResourceId;

    private ResourceType sourceResourceType;

    private String forkedResourceName;

    private Long forkedResourceOwnerId;

    private Integer forkedResourceVersion;

}
