package com.example.api.service;

import com.daml.ledger.javaapi.data.Transaction;
import com.example.api.domain.asset.AssetRequestResponse;
import com.example.api.domain.asset.ChangeAssetState;
import com.example.api.domain.asset.cash.Cash;
import com.example.api.domain.asset.cash.CashRequest;
import com.example.api.domain.entity.CashEntity;
import com.example.api.domain.entity.CashRequestEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.relational.core.query.Criteria;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CashService {
    Mono<Cash> getCashById(String id);

    Flux<Cash> cashFilter(Criteria criteria, Pageable pageable);

    Flux<CashRequest> cashRequestFilter(Criteria criteria, Pageable pageable);

    Mono<Long> cashFilterCount(Criteria criteria);

    Mono<Long> cashRequestFilterCount(Criteria criteria);
    Mono<CashEntity> insertOrUpdateCash(main.assets.Cash cash, String contractId);

    Mono<CashRequestEntity> insertCashRequest(main.assets.CashRequest cashRequest, String contractId);

    Mono<Void> deleteCash(String contractId);

    Mono<Void> deleteCashRequest(String contractId);

    Mono<Transaction> updateCashRequestAccept(AssetRequestResponse assetRequestResponse, String workflowId);

    Mono<Transaction> updateCashRequestDecline(AssetRequestResponse assetRequestResponse, String workflowId);

    Mono<Transaction> changeCashState(ChangeAssetState changeAssetState, String workflowId);
}
