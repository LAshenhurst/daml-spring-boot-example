package com.example.api.repository;

import com.example.api.domain.entity.ShareRequestEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ShareRequestRepository extends ReactiveCrudRepository<ShareRequestEntity, Long> {
    Mono<ShareRequestEntity> findByContractId(String contractId);

    Mono<Void> deleteByContractId(String contractId);
}
