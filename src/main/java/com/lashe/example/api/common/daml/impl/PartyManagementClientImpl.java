package com.lashe.example.api.common.daml.impl;

import com.daml.ledger.api.v1.admin.PartyManagementServiceGrpc;
import com.daml.ledger.api.v1.admin.PartyManagementServiceOuterClass;
import com.daml.ledger.rxjava.grpc.helpers.StubHelper;
import com.lashe.example.api.common.daml.PartyManagementClient;
import com.lashe.example.api.domain.daml.CreateLedgerParty;
import io.grpc.ManagedChannel;
import io.reactivex.Single;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class PartyManagementClientImpl implements PartyManagementClient {
    private final PartyManagementServiceGrpc.PartyManagementServiceFutureStub serviceStub;

    public PartyManagementClientImpl(ManagedChannel channel, Optional<String> accessToken) {
        this.serviceStub = StubHelper.authenticating(PartyManagementServiceGrpc.newFutureStub(channel), accessToken);
    }

    @Override
    public Mono<PartyManagementServiceOuterClass.AllocatePartyResponse> allocateParty(CreateLedgerParty createLedgerParty, String accessToken) {
        PartyManagementServiceOuterClass.AllocatePartyRequest request = PartyManagementServiceOuterClass.AllocatePartyRequest.newBuilder()
                .setDisplayName(createLedgerParty.getDisplayName())
                .setPartyIdHint(createLedgerParty.getIdentifierHint())
                .build();
        return RxJava2Adapter.singleToMono(Single.fromFuture(StubHelper.authenticating(this.serviceStub, Optional.ofNullable(accessToken)).allocateParty(request)));
    }

    @Override
    public Flux<PartyManagementServiceOuterClass.PartyDetails> getKnownParties(String accessToken) {
        PartyManagementServiceOuterClass.ListKnownPartiesRequest request = PartyManagementServiceOuterClass.ListKnownPartiesRequest.newBuilder().build();
        return RxJava2Adapter.singleToMono(Single.fromFuture(StubHelper.authenticating(this.serviceStub, Optional.ofNullable(accessToken)).listKnownParties(request)))
                .flatMapIterable(PartyManagementServiceOuterClass.ListKnownPartiesResponse::getPartyDetailsList);
    }
}
