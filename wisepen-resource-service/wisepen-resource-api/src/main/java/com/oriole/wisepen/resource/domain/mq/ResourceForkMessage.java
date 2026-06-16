package com.oriole.wisepen.resource.domain.mq;

import com.oriole.wisepen.resource.enums.MarketPurchaseType;
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

    private String orderId;

    private String sourceResourceId;

    private ResourceType resourceType;

    private MarketPurchaseType purchaseType;

    private Long version;

    private Long buyerId;

    private String resourceName;

    private String preview;

    private Long size;
}
