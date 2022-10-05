package com.lashe.example.api.common.daml.impl;

import com.daml.ledger.api.v1.admin.ParticipantPruningServiceGrpc;
import com.daml.ledger.api.v1.admin.ParticipantPruningServiceOuterClass;
import com.daml.ledger.rxjava.grpc.helpers.StubHelper;
import com.lashe.example.api.common.daml.PruningServiceClient;
import io.grpc.ManagedChannel;
import io.reactivex.Single;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class PruningServiceClientImpl implements PruningServiceClient {
    private final ParticipantPruningServiceGrpc.ParticipantPruningServiceFutureStub serviceStub;

    public PruningServiceClientImpl(ManagedChannel channel, Optional<String> accessToken) {
        this.serviceStub = StubHelper.authenticating(ParticipantPruningServiceGrpc.newFutureStub(channel), accessToken);
    }

    public Mono<ParticipantPruningServiceOuterClass.PruneResponse> prune(String pruneOffset, String accessToken) {
        ParticipantPruningServiceOuterClass.PruneRequest request = ParticipantPruningServiceOuterClass.PruneRequest.newBuilder().setPruneUpTo(pruneOffset).build();
        return RxJava2Adapter.singleToMono(Single.fromFuture(StubHelper.authenticating(this.serviceStub, Optional.ofNullable(accessToken)).prune(request)));
    }
}
