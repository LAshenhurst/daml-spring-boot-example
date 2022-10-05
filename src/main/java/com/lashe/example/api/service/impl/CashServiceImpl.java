package com.lashe.example.api.service.impl;

import com.daml.ledger.javaapi.data.Transaction;
import com.lashe.example.api.common.exceptions.ApiException;
import com.lashe.example.api.common.daml.LedgerClientAdapter;
import com.lashe.example.api.configuration.properties.LedgerProperties;
import com.lashe.example.api.domain.asset.AssetRequestResponse;
import com.lashe.example.api.domain.asset.ChangeAssetState;
import com.gft.example.api.domain.asset.cash.*;
import com.lashe.example.api.domain.asset.cash.Cash;
import com.lashe.example.api.domain.asset.cash.CashRequest;
import com.lashe.example.api.domain.entity.CashEntity;
import com.lashe.example.api.domain.entity.CashRequestEntity;
import com.lashe.example.api.domain.mapper.CashMapper;
import com.lashe.example.api.domain.mapper.LedgerMapper;
import com.lashe.example.api.repository.CashRepository;
import com.lashe.example.api.repository.CashRequestRepository;
import com.lashe.example.api.service.CashService;
import da.types.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashServiceImpl implements CashService {
    private final CashMapper cashMapper;
    private final LedgerMapper ledgerMapper;
    private final CashRepository cashRepository;
    private final CashRequestRepository cashRequestRepository;
    private final LedgerProperties ledgerProperties;
    private final LedgerClientAdapter ledgerClientAdapter;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    @Override
    public Flux<Cash> cashFilter(Criteria criteria, Pageable pageable) {
        Query filterQuery = Query.query(criteria).with(pageable);
        return r2dbcEntityTemplate.select(filterQuery, CashEntity.class)
                .map(cashMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing cashFilter service"))
                .doFinally(sub -> log.info("Executed cashFilter service"));
    }

    @Override
    public Flux<CashRequest> cashRequestFilter(Criteria criteria, Pageable pageable) {
        Query filterQuery = Query.query(criteria).with(pageable);
        return r2dbcEntityTemplate.select(filterQuery, CashRequestEntity.class)
                .map(cashMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing cashRequestFilter service"))
                .doFinally(sub -> log.info("Executed cashRequestFilter service"));
    }

    @Override
    public Mono<Long> cashFilterCount(Criteria criteria) {
        return r2dbcEntityTemplate.count(Query.query(criteria), CashEntity.class)
                .doOnSubscribe(sub -> log.info("Executing cashFilterCount service"))
                .doFinally(sub -> log.info("Executed cashFilterCount service"));
    }

    @Override
    public Mono<Long> cashRequestFilterCount(Criteria criteria) {
        return r2dbcEntityTemplate.count(Query.query(criteria), CashRequestEntity.class)
                .doOnSubscribe(sub -> log.info("Executing cashRequestFilterCount service"))
                .doFinally(sub -> log.info("Executed cashRequestFilterCount service"));
    }

    @Override
    public Mono<Cash> getCashById(String id) {
        return cashRepository.findByIdentifier(id)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Cash object not found.")))
                .map(cashMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing getCashById service"))
                .doFinally(sub -> log.info("Executed getCashById service"));
    }

    @Override
    public Mono<CashEntity> insertOrUpdateCash(main.assets.Cash cash, String contractId) {
        return cashRepository.findByIdentifier(cash.identifier)
                .switchIfEmpty(cashRepository.save(cashMapper.toEntity(cash, contractId)))
                .flatMap(entity -> {
                    if (entity.getLatestUpdateTimestamp() < cash.latestUpdateTimestamp) {
                        return cashRepository.save(cashMapper.toEntity(cash, contractId));
                    }
                    return Mono.just(entity);
                })
                .doOnSubscribe(sub -> log.debug("Processing Cash with id: {}", cash.identifier))
                .doOnSuccess(sub -> log.debug("Processed Cash with id: {}", cash.identifier));
    }

    @Override
    public Mono<Void> deleteCash(String contractId) { return cashRepository.deleteByContractId(contractId); }

    @Override
    public Mono<CashRequestEntity> insertCashRequest(main.assets.CashRequest cashRequest, String contractId) {
        return cashRequestRepository.findByContractId(contractId)
                .switchIfEmpty(cashRequestRepository.save(cashMapper.toEntity(cashRequest, contractId)))
                .doOnSubscribe(sub -> log.debug("Processing CashRequest with requester: {} for currency: {}", cashRequest.requester, cashRequest.currency))
                .doFinally(sub -> log.debug("Processed CashRequest with requester: {} for currency: {}", cashRequest.requester, cashRequest.currency));
    }

    @Override
    public Mono<Void> deleteCashRequest(String contractId) { return cashRequestRepository.deleteByContractId(contractId); }

    @Override
    public Mono<Transaction> updateCashRequestAccept(AssetRequestResponse assetRequestResponse, String workflowId) {
        return Mono.just(assetRequestResponse)
                .map(arResponse -> getPartyIdFromDisplayName(arResponse.getRequester()))
                .map(requesterPartyId -> new Tuple2<>(requesterPartyId, assetRequestResponse.getCurrency().getCurrencyCode()))
                .map(key -> main.assets.CashRequest.exerciseByKeyCreateCash(key, System.currentTimeMillis()))
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing updateCashRequestAccept service"))
                .doFinally(sub -> log.info("Executed updateCashRequestAccept service"));
    }

    @Override
    public Mono<Transaction> updateCashRequestDecline(AssetRequestResponse assetRequestResponse, String workflowId) {
        return Mono.just(assetRequestResponse)
                .map(arResponse -> getPartyIdFromDisplayName(arResponse.getRequester()))
                .map(requesterPartyId -> new Tuple2<>(requesterPartyId, assetRequestResponse.getCurrency().getCurrencyCode()))
                .map(main.assets.CashRequest::exerciseByKeyRejectCashRequest)
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing updateCashRequestDecline service"))
                .doFinally(sub -> log.info("Executed updateCashRequestDecline service"));
    }

    private String getPartyIdFromDisplayName(String displayName) {
        String partyId = ledgerProperties.getLedgerIdFromDisplayName(displayName);
        if (partyId == null) { throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid requester provided."); }
        return partyId;
    }

    @Override
    public Mono<Transaction> changeCashState(ChangeAssetState changeAssetState, String workflowId) {
        return Mono.just(changeAssetState)
                .map(caState -> new Tuple2<>(ledgerProperties.getCustodianPartyId(), caState.getIdentifier()))
                .map(key ->
                        main.assets.Cash.exerciseByKeyChangeCashState(
                                key,
                                Instant.now().getEpochSecond(),
                                changeAssetState.getAmount(),
                                ledgerMapper.toDaml(changeAssetState.getNewState())
                        )
                )
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing deleteCash service"))
                .doFinally(sub -> log.info("Executed deleteCash service"));
    }
}
