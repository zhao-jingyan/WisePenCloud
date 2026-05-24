package com.oriole.wisepen.resource.repository;

import com.oriole.wisepen.resource.domain.entity.ResourceInteractionInfoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ResourceInteractionInfoRepository extends MongoRepository<ResourceInteractionInfoEntity, String> {
    List<ResourceInteractionInfoEntity> findByResourceIdIn(List<String> resourceIds);

    void deleteAllByResourceIdIn(List<String> resourceIds);
}
