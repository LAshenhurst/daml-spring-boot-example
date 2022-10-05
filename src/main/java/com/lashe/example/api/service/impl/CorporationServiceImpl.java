package com.lashe.example.api.service.impl;

import com.daml.ledger.javaapi.data.Transaction;
import com.lashe.example.api.common.daml.LedgerClientAdapter;
import com.lashe.example.api.configuration.properties.LedgerProperties;
import com.lashe.example.api.domain.asset.cash.CreateCashRequest;
import com.lashe.example.api.domain.exchange.Offer;
import com.lashe.example.api.domain.asset.share.CreateShareRequest;
import com.lashe.example.api.helper.SecurityHelper;
import com.lashe.example.api.service.CorporationService;
import da.types.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.entities.Corporation;
import main.exchange.BuyOffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorporationServiceImpl implements CorporationService {
    private final LedgerClientAdapter ledgerClientAdapter;
    private final LedgerProperties ledgerProperties;

    @Override
    public Mono<Transaction> createShareRequest(CreateShareRequest createShareRequest, String workflowId) {
        return SecurityHelper.getUsername()
                .map(username -> new Tuple2<>(ledgerProperties.getCustodianPartyId(), ledgerProperties.getLedgerIdFromDisplayName(username)))
                .map(key -> Corporation.exerciseByKeyCreateShareRequest(key, BigDecimal.valueOf(createShareRequest.getValue())))
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing createShareRequest service"))
                .doFinally(sub -> log.info("Executed createShareRequest service"));
    }

    @Override
    public Mono<Transaction> createCashRequest(CreateCashRequest createCashRequest, String workflowId) {
        return SecurityHelper.getUsername()
                .map(username -> new Tuple2<>(ledgerProperties.getCustodianPartyId(), ledgerProperties.getLedgerIdFromDisplayName(username)))
                .map(key -> Corporation.exerciseByKeyCorporationCashRequest(key, BigDecimal.valueOf(createCashRequest.getAmount()), createCashRequest.getCurrency().getCurrencyCode()))
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing createCashRequest service"))
                .doFinally(sub -> log.info("Executed createCashRequest service"));
    }

    @Override
    public Mono<Transaction> createBuyOffer(Offer offer, String workflowId) {
        return SecurityHelper.getUsername()
                .map(username -> new Tuple2<>(ledgerProperties.getCustodianPartyId(), ledgerProperties.getLedgerIdFromDisplayName(username)))
                .map(key ->
                        Corporation.exerciseByKeyCorpOfferBuy(
                                key,
                                BigDecimal.valueOf(offer.getPricePerShare()),
                                ledgerProperties.getLedgerIdFromDisplayName(offer.getCorp()),
                                offer.getAmount(),
                                offer.getCurrency(),
                                Instant.now().getEpochSecond()
                        )
                )
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing createBuyOffer service"))
                .doFinally(sub -> log.info("Executed createBuyOffer service"));
    }

    @Override
    public Mono<Transaction> createSellOffer(Offer offer, String workflowId) {
        return SecurityHelper.getUsername()
                .map(username -> new Tuple2<>(ledgerProperties.getCustodianPartyId(), ledgerProperties.getLedgerIdFromDisplayName(username)))
                .map(key ->
                        Corporation.exerciseByKeyCorpOfferSell(
                                key,
                                ledgerProperties.getLedgerIdFromDisplayName(offer.getCorp()),
                                offer.getAmount(),
                                BigDecimal.valueOf(offer.getPricePerShare()),
                                offer.getCurrency(),
                                Instant.now().getEpochSecond()
                        )
                )
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing createSellOffer service"))
                .doFinally(sub -> log.info("Executed createSellOffer service"));
    }
}