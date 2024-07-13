package com.example.api.service;

import com.daml.ledger.javaapi.data.Transaction;
import com.example.api.domain.asset.AssetRequestResponse;
import com.example.api.domain.asset.ChangeAssetState;
import com.example.api.domain.asset.share.Share;
import com.example.api.domain.asset.share.ShareRequest;
import com.example.api.domain.entity.ShareEntity;
import com.example.api.domain.entity.ShareRequestEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.relational.core.query.Criteria;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ShareService {
    Mono<Share> getSharesById(String id);

    Flux<Share> shareFilter(Criteria criteria, Pageable pageable);

    Flux<ShareRequest> shareRequestFilter(Criteria criteria, Pageable pageable);

    Mono<Long> shareFilterCount(Criteria criteria);

    Mono<Long> shareRequestFilterCount(Criteria criteria);

    Mono<ShareEntity> insertOrUpdateShares(main.assets.Shares share, String contractId);

    Mono<ShareRequestEntity> insertShareRequest(main.assets.ShareRequest shareRequest, String contractId);

    Mono<Void> deleteShares(String contractId);

    Mono<Void> deleteShareRequest(String contractId);

    Mono<Transaction> updateShareRequestAccept(AssetRequestResponse assetRequestResponse, String workflowId);

    Mono<Transaction> updateShareRequestDecline(AssetRequestResponse assetRequestResponse, String workflowId);

    Mono<Transaction> changeShareState(ChangeAssetState changeAssetState, String workflowId);
}
