package com.example.api.repository;

import com.example.api.domain.entity.CashRequestEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface CashRequestRepository extends ReactiveCrudRepository<CashRequestEntity, Long> {
    Mono<CashRequestEntity> findByContractId(String contractId);

    Mono<Void> deleteByContractId(String contractId);
}
