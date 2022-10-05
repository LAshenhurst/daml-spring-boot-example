package com.lashe.example.api.common.daml.impl;

import com.daml.ledger.api.v1.admin.PackageManagementServiceGrpc;
import com.daml.ledger.api.v1.admin.PackageManagementServiceOuterClass;
import com.daml.ledger.rxjava.grpc.helpers.StubHelper;
import com.lashe.example.api.common.daml.PackageManagementClient;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.reactivex.Single;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class PackageManagementClientImpl implements PackageManagementClient {
    private final PackageManagementServiceGrpc.PackageManagementServiceFutureStub serviceStub;

    public PackageManagementClientImpl(ManagedChannel channel, Optional<String> accessToken) {
        this.serviceStub = StubHelper.authenticating(PackageManagementServiceGrpc.newFutureStub(channel), accessToken);
    }

    @Override
    public Flux<PackageManagementServiceOuterClass.PackageDetails> getKnownPackages(String accessToken) {
        PackageManagementServiceOuterClass.ListKnownPackagesRequest request = PackageManagementServiceOuterClass.ListKnownPackagesRequest.newBuilder().build();
        return RxJava2Adapter.singleToMono(Single.fromFuture(StubHelper.authenticating(this.serviceStub, Optional.ofNullable(accessToken)).listKnownPackages(request)))
                .flatMapMany(response -> Flux.fromIterable(response.getPackageDetailsList()));
    }

    @Override
    public Mono<PackageManagementServiceOuterClass.UploadDarFileResponse> uploadDarFile(String accessToken, ByteString darFile) {
        PackageManagementServiceOuterClass.UploadDarFileRequest request = PackageManagementServiceOuterClass.UploadDarFileRequest.newBuilder().setDarFile(darFile).build();
        return RxJava2Adapter.singleToMono(Single.fromFuture(StubHelper.authenticating(this.serviceStub, Optional.ofNullable(accessToken)).uploadDarFile(request)));
    }
}
