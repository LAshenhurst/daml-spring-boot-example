package com.lashe.example.api.common.daml;

import com.daml.ledger.api.v1.admin.PartyManagementServiceOuterClass;
import com.lashe.example.api.domain.daml.CreateLedgerParty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PartyManagementClient {
    Mono<PartyManagementServiceOuterClass.AllocatePartyResponse> allocateParty(CreateLedgerParty createLedgerParty, String accessToken);

    Flux<PartyManagementServiceOuterClass.PartyDetails> getKnownParties(String accessToken);
}
