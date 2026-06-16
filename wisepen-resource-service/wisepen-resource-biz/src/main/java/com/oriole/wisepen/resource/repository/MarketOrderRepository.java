package com.oriole.wisepen.resource.repository;

import com.oriole.wisepen.resource.domain.entity.MarketOrderEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketOrderRepository extends MongoRepository<MarketOrderEntity, String> {

    Optional<MarketOrderEntity> findByTradeTraceId(String tradeTraceId);

    Page<MarketOrderEntity> findByBuyerId(String buyerId, Pageable pageable);
}
