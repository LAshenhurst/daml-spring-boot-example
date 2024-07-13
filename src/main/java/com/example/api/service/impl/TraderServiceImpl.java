package com.example.api.service.impl;

import com.daml.ledger.javaapi.data.Transaction;
import com.example.api.common.daml.LedgerClientAdapter;
import com.example.api.configuration.properties.LedgerProperties;
import com.example.api.domain.asset.cash.CreateCashRequest;
import com.example.api.domain.exchange.Offer;
import com.example.api.helper.SecurityHelper;
import com.example.api.service.TraderService;
import da.types.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.entities.Trader;
import main.exchange.BuyOffer;
import main.exchange.SellOffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TraderServiceImpl implements TraderService {
    private final LedgerClientAdapter ledgerClientAdapter;
    private final LedgerProperties ledgerProperties;

    public Mono<Transaction> createCashRequest(CreateCashRequest createCashRequest, String workflowId) {
        return SecurityHelper.getUsername()
                .map(username -> new Tuple2<>(ledgerProperties.getCustodianPartyId(), ledgerProperties.getLedgerIdFromDisplayName(username)))
                .map(key -> Trader.exerciseByKeyTraderCashRequest(key, BigDecimal.valueOf(createCashRequest.getAmount()), createCashRequest.getCurrency().getCurrencyCode()))
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing createTraderCashRequest service"))
                .doFinally(sub -> log.info("Executed createTraderCashRequest service"));
    }

    public Mono<Transaction> createBuyOffer(Offer offer, String workflowId) {
        return SecurityHelper.getUsername()
                .map(username -> new Tuple2<>(ledgerProperties.getCustodianPartyId(), ledgerProperties.getLedgerIdFromDisplayName(username)))
                .map(key ->
                        Trader.exerciseByKeyTraderOfferBuy(
                                key,
                                BigDecimal.valueOf(offer.getPricePerShare()),
                                ledgerProperties.getLedgerIdFromDisplayName(offer.getCorp()),
                                offer.getAmount(),
                                offer.getCurrency(),
                                Instant.now().getEpochSecond()
                        )
                )
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing createTraderBuyOffer service"))
                .doFinally(sub -> log.info("Executed createTraderBuyOffer service"));
    }



    public Mono<Transaction> createSellOffer(Offer offer, String workflowId) {
        return SecurityHelper.getUsername()
                .map(username -> new Tuple2<>(ledgerProperties.getCustodianPartyId(), ledgerProperties.getLedgerIdFromDisplayName(username)))
                .map(key ->
                        Trader.exerciseByKeyTraderOfferSell(
                                key,
                                ledgerProperties.getLedgerIdFromDisplayName(offer.getCorp()),
                                offer.getAmount(),
                                BigDecimal.valueOf(offer.getPricePerShare()),
                                offer.getCurrency(),
                                Instant.now().getEpochSecond()
                        )
                )
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing createTraderSellOffer service"))
                .doFinally(sub -> log.info("Executed createTraderSellOffer service"));
    }


}