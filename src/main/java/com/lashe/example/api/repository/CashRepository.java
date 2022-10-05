package com.lashe.example.api.repository;

import com.lashe.example.api.domain.entity.CashEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CashRepository extends ReactiveCrudRepository<CashEntity, Long> {
    Mono<CashEntity> findByIdentifier(String identifier);

    Mono<Void> deleteByContractId(String contractId);
}