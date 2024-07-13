package com.example.api.repository;

import com.example.api.domain.entity.ShareEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ShareRepository extends ReactiveCrudRepository<ShareEntity, Long> {
    Mono<ShareEntity> findByIdentifier(String identifier);

    Mono<Void> deleteByContractId(String contractId);
}