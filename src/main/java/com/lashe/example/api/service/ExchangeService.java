package com.lashe.example.api.service;

import com.daml.ledger.javaapi.data.Transaction;
import com.lashe.example.api.domain.entity.BuyOfferEntity;
import com.lashe.example.api.domain.entity.SellOfferEntity;
import com.lashe.example.api.domain.exchange.BuyOffer;
import com.lashe.example.api.domain.exchange.SellOffer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.relational.core.query.Criteria;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ExchangeService {
    Mono<BuyOfferEntity> insertOrUpdateBuyOffer(main.exchange.BuyOffer buyOffer, String contractId);

    Mono<Void> deleteBuyOffer(String contractId);

    Mono<SellOfferEntity> insertOrUpdateSellOffer(main.exchange.SellOffer sellOffer, String contractId);

    Mono<Void> deleteSellOffer(String contractId);

    Flux<SellOffer> sellOfferFilter(Criteria criteria, Pageable pageable);

    Mono<Long> sellOfferFilterCount(Criteria criteria);

    Mono<SellOffer> getSellOfferById(String id);

    Flux<BuyOffer> buyOfferFilter(Criteria criteria, Pageable pageable);

    Mono<Long> buyOfferFilterCount(Criteria criteria);

    Mono<BuyOffer> getBuyOfferById(String id);

    Mono<Transaction> cancelBuyOffer(String id, String workflowId);

    Mono<Transaction> cancelSellOffer(String id, String workflowId);
}
