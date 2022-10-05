package com.lashe.example.api.repository;

import com.lashe.example.api.domain.entity.BuyOfferEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface BuyOfferRepository extends ReactiveCrudRepository<BuyOfferEntity, Long> {
    Mono<BuyOfferEntity> findByIdentifier(String identifier);

    Mono<Void> deleteByContractId(String contractId);
}