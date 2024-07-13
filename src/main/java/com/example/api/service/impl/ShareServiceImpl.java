package com.example.api.service.impl;

import com.daml.ledger.javaapi.data.Transaction;
import com.example.api.common.exceptions.ApiException;
import com.example.api.common.daml.LedgerClientAdapter;
import com.example.api.configuration.properties.LedgerProperties;
import com.example.api.domain.asset.AssetRequestResponse;
import com.example.api.domain.asset.ChangeAssetState;
import com.example.api.domain.asset.share.Share;
import com.example.api.domain.asset.share.ShareRequest;
import com.example.api.domain.entity.ShareEntity;
import com.example.api.domain.entity.ShareRequestEntity;
import com.example.api.domain.mapper.LedgerMapper;
import com.example.api.domain.mapper.ShareMapper;
import com.example.api.repository.ShareRepository;
import com.example.api.repository.ShareRequestRepository;
import com.example.api.service.ShareService;
import da.types.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.assets.Shares;
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
public class ShareServiceImpl implements ShareService {
    private final LedgerClientAdapter ledgerClientAdapter;
    private final LedgerProperties ledgerProperties;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final ShareRepository shareRepository;
    private final ShareRequestRepository shareRequestRepository;
    private final ShareMapper shareMapper;
    private final LedgerMapper ledgerMapper;

    @Override
    public Mono<Share> getSharesById(String id) {
        return shareRepository.findByIdentifier(id)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Shares not found.")))
                .map(shareMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing getSharesById service"))
                .doFinally(sub -> log.info("Executed getSharesById service"));
    }

    @Override
    public Flux<Share> shareFilter(Criteria criteria, Pageable pageable) {
        Query filterQuery = Query.query(criteria).with(pageable);
        return r2dbcEntityTemplate.select(filterQuery, ShareEntity.class)
                .map(shareMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing shareFilter service"))
                .doFinally(sub -> log.info("Executed shareFilter service"));
    }

    @Override
    public Flux<ShareRequest> shareRequestFilter(Criteria criteria, Pageable pageable) {
        Query filterQuery = Query.query(criteria).with(pageable);
        return r2dbcEntityTemplate.select(filterQuery, ShareRequestEntity.class)
                .map(shareMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing shareRequestFilter service"))
                .doFinally(sub -> log.info("Executed shareRequestFilter service"));
    }

    @Override
    public Mono<Long> shareFilterCount(Criteria criteria) {
        return r2dbcEntityTemplate.count(Query.query(criteria), ShareEntity.class)
                .doOnSubscribe(sub -> log.info("Executing shareFilterCount service"))
                .doFinally(sub -> log.info("Executed shareFilterCount service"));
    }

    @Override
    public Mono<Long> shareRequestFilterCount(Criteria criteria) {
        return r2dbcEntityTemplate.count(Query.query(criteria), ShareRequestEntity.class)
                .doOnSubscribe(sub -> log.info("Executing shareRequestFilterCount service"))
                .doFinally(sub -> log.info("Executed shareRequestFilterCount service"));
    }

    @Override
    public Mono<ShareEntity> insertOrUpdateShares(Shares share, String contractId) {
        return shareRepository.findByIdentifier(share.identifier)
                .switchIfEmpty(shareRepository.save(shareMapper.toEntity(share, contractId)))
                .flatMap(shareEntity -> {
                    if (shareEntity.getLatestUpdateTimestamp() < share.latestUpdateTimestamp) {
                        return shareRepository.save(shareMapper.toEntity(share, contractId));
                    }
                    return Mono.just(shareEntity);
                })
                .doOnSubscribe(sub -> log.debug("Processing Share with id: {}", share.identifier))
                .doOnSuccess(sub -> log.debug("Processed Share with id: {}", share.identifier));
    }

    @Override
    public Mono<ShareRequestEntity> insertShareRequest(main.assets.ShareRequest shareRequest, String contractId) {
        return shareRequestRepository.findByContractId(contractId)
                .switchIfEmpty(shareRequestRepository.save(shareMapper.toEntity(shareRequest, contractId)))
                .doOnSubscribe(sub -> log.debug("Processing ShareRequest for requester: {}", shareRequest.requester))
                .doFinally(sub -> log.debug("Processed ShareRequest for requester: {}", shareRequest.requester));
    }

    @Override
    public Mono<Void> deleteShares(String contractId) {
        return shareRepository.deleteByContractId(contractId);
    }

    @Override
    public Mono<Void> deleteShareRequest(String contractId) { return shareRequestRepository.deleteByContractId(contractId); }

    @Override
    public Mono<Transaction> updateShareRequestAccept(AssetRequestResponse assetRequestResponse, String workflowId) {
        return Mono.just(ledgerProperties.getLedgerIdFromDisplayName(assetRequestResponse.getRequester()))
                .map(key -> main.assets.ShareRequest.exerciseByKeyCreateShares(key, Instant.now().getEpochSecond()))
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing updateShareRequestAccept service"))
                .doFinally(sub -> log.info("Executed updateShareRequestAccept service"));
    }

    @Override
    public Mono<Transaction> updateShareRequestDecline(AssetRequestResponse assetRequestResponse, String workflowId) {
        return Mono.just(ledgerProperties.getLedgerIdFromDisplayName(assetRequestResponse.getRequester()))
                .map(main.assets.ShareRequest::exerciseByKeyRejectShareRequest)
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing updateShareRequestDecline service"))
                .doFinally(sub -> log.info("Executed updateShareRequestDecline service"));
    }

    @Override
    public Mono<Transaction> changeShareState(ChangeAssetState changeAssetState, String workflowId) {
        return Mono.just(changeAssetState.getIdentifier())
                .map(identifier -> new Tuple2<>(ledgerProperties.getCustodianPartyId(), identifier))
                .map(key ->
                        Shares.exerciseByKeyChangeShareState(
                                key,
                                Instant.now().getEpochSecond(),
                                changeAssetState.getAmount(),
                                ledgerMapper.toDaml(changeAssetState.getNewState())
                        )
                )
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing revokeShares service"))
                .doFinally(sub -> log.info("Executed revokeShares service"));
    }
}
