package com.oriole.wisepen.resource.repository;

import com.oriole.wisepen.resource.domain.entity.ResourceUserInteractionRecordEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ResourceUserInteractRecordRepository extends MongoRepository<ResourceUserInteractionRecordEntity, String> {
    Optional<ResourceUserInteractionRecordEntity> findByUserIdAndResourceId(String userId, String resourceId);

    void deleteAllByResourceIdIn(List<String> resourceIds);
}