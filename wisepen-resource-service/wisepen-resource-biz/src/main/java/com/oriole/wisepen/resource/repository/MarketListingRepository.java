package com.oriole.wisepen.resource.repository;

import com.oriole.wisepen.resource.domain.entity.MarketListingEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketListingRepository extends MongoRepository<MarketListingEntity, String> {

    MarketListingEntity findByMarketGroupIdAndSourceResourceId(String marketGroupId, String sourceResourceId);

    Page<MarketListingEntity> findBySellerId(String sellerId, Pageable pageable);
}
