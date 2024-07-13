package com.example.api.service.impl;

import com.daml.ledger.javaapi.data.Transaction;
import com.example.api.common.daml.LedgerClientAdapter;
import com.example.api.common.exceptions.ApiException;
import com.example.api.configuration.properties.LedgerProperties;
import com.example.api.domain.entity.BuyOfferEntity;
import com.example.api.domain.entity.SellOfferEntity;
import com.example.api.domain.exchange.BuyOffer;
import com.example.api.domain.exchange.SellOffer;
import com.example.api.domain.mapper.BuyOfferMapper;
import com.example.api.domain.mapper.SellOfferMapper;
import com.example.api.repository.BuyOfferRepository;
import com.example.api.repository.SellOfferRepository;
import com.example.api.service.ExchangeService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeServiceImpl implements ExchangeService {
    private final SellOfferRepository sellOfferRepository;
    private final BuyOfferRepository buyOfferRepository;
    private final BuyOfferMapper buyOfferMapper;
    private final SellOfferMapper sellOfferMapper;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final LedgerProperties ledgerProperties;
    private final LedgerClientAdapter ledgerClientAdapter;

    @Override
    public Mono<BuyOfferEntity> insertOrUpdateBuyOffer(main.exchange.BuyOffer buyOffer, String contractId) {
        return buyOfferRepository.findByIdentifier(buyOffer.id)
                .switchIfEmpty(buyOfferRepository.save(buyOfferMapper.toEntity(buyOffer, contractId)))
                .flatMap(existingEntity -> {
                    BuyOfferEntity newEntity = buyOfferMapper.toEntity(buyOffer, contractId);
                    if (existingEntity.getLatestUpdateTimestamp() < newEntity.getLatestUpdateTimestamp()) {
                        return buyOfferRepository.save(newEntity);
                    }
                    return Mono.just(existingEntity);
                })
                .doOnSubscribe(sub -> log.debug("Processing BuyOffer with id: {}", buyOffer.id))
                .doOnSuccess(sub -> log.debug("Processed BuyOffer with id: {}", buyOffer.id));
    }

    @Override
    public Mono<Void> deleteBuyOffer(String contractId) {
        return buyOfferRepository.deleteByContractId(contractId);
    }

    @Override
    public Mono<SellOfferEntity> insertOrUpdateSellOffer(main.exchange.SellOffer sellOffer, String contractId) {
        return sellOfferRepository.findByIdentifier(sellOffer.id)
                .switchIfEmpty(sellOfferRepository.save(sellOfferMapper.toEntity(sellOffer, contractId)))
                .flatMap(existingEntity -> {
                    SellOfferEntity newEntity = sellOfferMapper.toEntity(sellOffer, contractId);
                    if (existingEntity.getLatestUpdateTimestamp() < newEntity.getLatestUpdateTimestamp()) {
                        return sellOfferRepository.save(newEntity);
                    }
                    return Mono.just(existingEntity);
                })
                .doOnSubscribe(sub -> log.debug("Processing SellOffer with id: {}", sellOffer.id))
                .doOnSuccess(sub -> log.debug("Processed SellOffer with id: {}", sellOffer.id));
    }

    @Override
    public Mono<Void> deleteSellOffer(String contractId) {
        return sellOfferRepository.deleteByContractId(contractId);
    }

    @Override
    public Flux<SellOffer> sellOfferFilter(Criteria criteria, Pageable pageable) {
        Query filterQuery = Query.query(criteria).with(pageable);
        return r2dbcEntityTemplate.select(filterQuery, SellOfferEntity.class)
                .map(sellOfferMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing sellOfferFilter service"))
                .doFinally(sub -> log.info("Executed sellOfferFilter service"));
    }

    @Override
    public Mono<Long> sellOfferFilterCount(Criteria criteria) {
        return r2dbcEntityTemplate.count(Query.query(criteria), SellOfferEntity.class)
                .doOnSubscribe(sub -> log.info("Executing sellOfferFilterCount service"))
                .doFinally(sub -> log.info("Executed sellOfferFilterCount service"));
    }

    @Override
    public Mono<SellOffer> getSellOfferById(String id) {
        return sellOfferRepository.findByIdentifier(id)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Sell Offer with id '" + id + "' not found.")))
                .map(sellOfferMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing getSellOfferById service"))
                .doFinally(sub -> log.info("Executed getSellOfferById service"));
    }

    @Override
    public Flux<BuyOffer> buyOfferFilter(Criteria criteria, Pageable pageable) {
        Query filterQuery = Query.query(criteria).with(pageable);
        return r2dbcEntityTemplate.select(filterQuery, BuyOfferEntity.class)
                .map(buyOfferMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing buyOfferFilter service"))
                .doFinally(sub -> log.info("Executed buyOfferFilter service"));
    }

    @Override
    public Mono<Long> buyOfferFilterCount(Criteria criteria) {
        return r2dbcEntityTemplate.count(Query.query(criteria), BuyOfferEntity.class)
                .doOnSubscribe(sub -> log.info("Executing buyOfferFilterCount service"))
                .doFinally(sub -> log.info("Executing buyOfferFilterCount service"));
    }

    @Override
    public Mono<BuyOffer> getBuyOfferById(String id) {
        return buyOfferRepository.findByIdentifier(id)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Buy Offer with id '" + id + "' not found.")))
                .map(buyOfferMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing getBuyOfferById service"))
                .doFinally(sub -> log.info("Executed getBuyOfferById service"));
    }

    @Override
    public Mono<Transaction> cancelBuyOffer(String id, String workflowId) {
        return Mono.just(new Tuple2<>(ledgerProperties.getCustodianPartyId(), id))
                .map(main.exchange.BuyOffer::exerciseByKeyCancelBuyOffer)
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing cancelBuyOffer service"))
                .doFinally(sub -> log.info("Executed cancelBuyOffer service"));
    }

    @Override
    public Mono<Transaction> cancelSellOffer(String id, String workflowId) {
        return Mono.just(new Tuple2<>(ledgerProperties.getCustodianPartyId(), id))
                .map(main.exchange.SellOffer::exerciseByKeyCancelSellOffer)
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing cancelBuyOffer service"))
                .doFinally(sub -> log.info("Executed cancelBuyOffer service"));
    }
}