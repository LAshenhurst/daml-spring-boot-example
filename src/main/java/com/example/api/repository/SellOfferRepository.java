package com.example.api.repository;

import com.example.api.domain.entity.SellOfferEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface SellOfferRepository extends ReactiveCrudRepository<SellOfferEntity, Long> {
    Mono<SellOfferEntity> findByIdentifier(String identifier);

    Mono<Void> deleteByContractId(String contractId);
}