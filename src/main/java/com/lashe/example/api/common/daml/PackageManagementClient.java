package com.lashe.example.api.common.daml;

import com.daml.ledger.api.v1.admin.PackageManagementServiceOuterClass;
import com.google.protobuf.ByteString;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PackageManagementClient {
    Flux<PackageManagementServiceOuterClass.PackageDetails> getKnownPackages(String accessToken);

    Mono<PackageManagementServiceOuterClass.UploadDarFileResponse> uploadDarFile(String accessToken, ByteString darFile);
}
