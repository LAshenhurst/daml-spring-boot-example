package com.example.api.service;

import com.daml.ledger.javaapi.data.Transaction;
import com.example.api.domain.asset.cash.CreateCashRequest;
import com.example.api.domain.exchange.Offer;
import reactor.core.publisher.Mono;

public interface TraderService {
    Mono<Transaction> createCashRequest(CreateCashRequest createCashRequest, String workflowId);

    Mono<Transaction> createBuyOffer(Offer offer, String workflowId);

    Mono<Transaction> createSellOffer(Offer offer, String workflowId);
}