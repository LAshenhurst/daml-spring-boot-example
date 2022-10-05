package com.lashe.example.api.service;

import com.lashe.example.api.domain.Resource;
import com.lashe.example.api.domain.daml.CreateLedgerParty;
import com.lashe.example.api.domain.daml.LedgerParty;
import com.lashe.example.api.domain.daml.PackageDetails;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LedgerManagementService {
    Flux<LedgerParty> getLedgerParties();

    Mono<LedgerParty> getLedgerPartyById(String id);

    Mono<LedgerParty> createLedgerParty(CreateLedgerParty createLedgerParty);

    Mono<Resource> pruneLedger(String pruneOffset);

    Mono<PackageDetails> getPackageById(String id);

    Flux<PackageDetails> getPackages();

    Mono<Resource> uploadDarFile(DataBuffer dataBuffer);

    Mono<Boolean> manualLedgerConnect();
}
