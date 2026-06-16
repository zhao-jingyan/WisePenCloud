package com.oriole.wisepen.resource.mq;

import com.oriole.wisepen.resource.domain.entity.ResourceItemEntity;
import com.oriole.wisepen.resource.domain.mq.ResourceForkMessage;

import java.util.List;

public interface IResourceEventPublisher {
    void publishAclRecalculateEvent(String resourceId, String triggerSource);
    void publishResDeletedEvent(List<ResourceItemEntity> resourceList);
    void publishResourceForkEvent(ResourceForkMessage message);
}
