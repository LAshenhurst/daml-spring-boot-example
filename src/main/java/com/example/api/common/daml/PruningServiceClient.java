package com.example.api.common.daml;

import com.daml.ledger.api.v1.admin.ParticipantPruningServiceOuterClass;
import reactor.core.publisher.Mono;

public interface PruningServiceClient {
    Mono<ParticipantPruningServiceOuterClass.PruneResponse> prune(String pruneOffset, String accessToken);
}
