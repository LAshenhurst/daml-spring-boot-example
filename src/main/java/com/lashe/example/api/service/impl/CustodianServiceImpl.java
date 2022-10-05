package com.lashe.example.api.service.impl;

import com.daml.ledger.javaapi.data.Transaction;
import com.lashe.example.api.common.daml.LedgerClientAdapter;
import com.lashe.example.api.configuration.properties.LedgerProperties;
import com.lashe.example.api.domain.CreateCorporation;
import com.lashe.example.api.domain.CreateTrader;
import com.lashe.example.api.domain.exchange.MatchOffers;
import com.lashe.example.api.service.CustodianService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.entities.Custodian;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustodianServiceImpl implements CustodianService {
    private final LedgerClientAdapter ledgerClientAdapter;
    private final LedgerProperties ledgerProperties;

    @Override
    public Mono<Transaction> createCustodian(String workflowId) {
        return Mono.just(ledgerProperties.getCustodianPartyId())
                .map(Custodian::create)
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing createCustodian service"))
                .doFinally(sub -> log.info("Executed createCustodian service"));
    }

    @Override
    public Mono<Transaction> createCorporation(CreateCorporation createCorporation, String workflowId) {
        return Mono.just(ledgerProperties.getLedgerIdFromDisplayName(createCorporation.getCorporation()))
                .map(corp -> Custodian.exerciseByKeyRegisterCorporation(ledgerProperties.getCustodianPartyId(), corp))
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing createCorporation service"))
                .doFinally(sub -> log.info("Executed createCorporation service"));
    }

    @Override
    public Mono<Transaction> createTrader(CreateTrader createTrader, String workflowId) {
        return Mono.just(ledgerProperties.getLedgerIdFromDisplayName(createTrader.getTrader()))
                .map(trader -> Custodian.exerciseByKeyRegisterTrader(ledgerProperties.getCustodianPartyId(), trader))
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing createTrader service"))
                .doFinally(sub -> log.info("Executed createTrader service"));
    }

    @Override
    public Mono<Transaction> exerciseMatch(MatchOffers matchOffers, String workflowId) {
        return Mono.just(matchOffers)
                .map(match ->
                        Custodian.exerciseByKeyMatch(
                                ledgerProperties.getCustodianPartyId(),
                                match.getSellOfferIdentifier(),
                                match.getBuyOfferIdentifier(),
                                Instant.now().getEpochSecond()
                        )
                )
                .flatMap(command -> ledgerClientAdapter.submitCommand(workflowId, command))
                .doOnSubscribe(sub -> log.info("Executing exerciseMatch service"))
                .doFinally(sub -> log.info("Executed exerciseMatch service"));
    }
}