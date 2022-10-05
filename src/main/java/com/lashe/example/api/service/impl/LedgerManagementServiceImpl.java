package com.lashe.example.api.service.impl;

import com.daml.ledger.api.v1.admin.PartyManagementServiceOuterClass;
import com.lashe.example.api.common.exceptions.ApiException;
import com.lashe.example.api.common.AppConstants;
import com.lashe.example.api.common.daml.LedgerClient;
import com.lashe.example.api.configuration.properties.LedgerProperties;
import com.lashe.example.api.domain.Resource;
import com.lashe.example.api.domain.daml.CreateLedgerParty;
import com.lashe.example.api.domain.daml.LedgerParty;
import com.lashe.example.api.domain.daml.PackageDetails;
import com.lashe.example.api.domain.mapper.LedgerMapper;
import com.lashe.example.api.helper.LedgerHelper;
import com.lashe.example.api.listener.LedgerListener;
import com.lashe.example.api.service.LedgerManagementService;
import com.google.protobuf.ByteString;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class LedgerManagementServiceImpl implements LedgerManagementService {
    private final LedgerMapper ledgerMapper;
    private final LedgerClient ledgerClient;
    private final LedgerListener ledgerListener;
    private final LedgerProperties ledgerProperties;

    @Override
    public Flux<LedgerParty> getLedgerParties() {
        return ledgerClient.getPartyManagementClient()
                .getKnownParties(null)
                .map(ledgerMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing getLedgerParties service"))
                .doFinally(sub -> log.info("Executed getLedgerParties service"));
    }

    @Override
    public Mono<LedgerParty> getLedgerPartyById(String id) {
        return ledgerClient.getPartyManagementClient()
                .getKnownParties(null)
                .filter(partyDetails -> partyDetails.getParty().equalsIgnoreCase(id))
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Party not found.")))
                .single()
                .map(ledgerMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing getLedgerPartyById service"))
                .doFinally(sub -> log.info("Executed getLedgerPartyById service"));
    }

    @Override
    public Mono<LedgerParty> createLedgerParty(CreateLedgerParty createLedgerParty) {
        return ledgerClient.getPartyManagementClient()
                .allocateParty(createLedgerParty, null)
                .map(response -> {
                    PartyManagementServiceOuterClass.PartyDetails newParty = response.getPartyDetails();
                    ledgerProperties.getLedgerPartyIdentifierMap().put(newParty.getParty(), newParty.getDisplayName());
                    return ledgerMapper.toDomain(newParty);
                })
                .doOnError(ex -> {
                    if (ex.getMessage().contains("Party already exists")) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Ledger already exists.");
                    }
                    if (ex.getMessage().contains("invalid arguments")) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Party Details provided, only '-' and '_' special characters are permitted.");
                    }
                    log.warn("Unexpected createLedgerParty error", ex);
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, AppConstants.DEFAULT_ERROR_MESSAGE);
                })
                .doOnSubscribe(sub -> log.info("Executing createLedgerParty service"))
                .doFinally(sub -> log.info("Executed createLedgerParty service"));
    }

    @Override
    public Mono<Resource> pruneLedger(String pruneOffset) {
        return ledgerClient.getPruningServiceClient()
                .prune(pruneOffset, null)
                .doOnError(ex -> {
                    if (ex.getMessage().contains("INVALID_ARGUMENT")) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Prune Offset provided.");
                    }
                    log.warn("Unexpected pruneLedger error", ex);
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, AppConstants.DEFAULT_ERROR_MESSAGE);
                })
                .map(response -> new Resource("Prune successful!"))
                .doOnSubscribe(sub -> log.info("Executing pruneLedger service"))
                .doFinally(sub -> log.info("Executed pruneLedger service"));
    }

    @Override
    public Mono<PackageDetails> getPackageById(String id) {
        return ledgerClient.getPackageManagementClient()
                .getKnownPackages(null)
                .filter(packageDetails -> packageDetails.getPackageId().equalsIgnoreCase(id))
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Package with id '" + id + "' not found.")))
                .single()
                .map(ledgerMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing getPackageById service"))
                .doFinally(sub -> log.info("Executed getPackageById service"));
    }

    @Override
    public Flux<PackageDetails> getPackages() {
        return ledgerClient.getPackageManagementClient()
                .getKnownPackages(null)
                .map(ledgerMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing getPackages service"))
                .doFinally(sub -> log.info("Executed getPackages service"));
    }

    @Override
    public Mono<Resource> uploadDarFile(@RequestParam("file") DataBuffer dataBuffer) {
        return ledgerClient.getPackageManagementClient()
                .uploadDarFile(null, ByteString.copyFrom(dataBuffer.asByteBuffer()))
                .map(response -> new Resource("Upload complete."))
                .doOnError(ex -> {
                    log.error("Error when uploading Dar file", ex);
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, AppConstants.DEFAULT_ERROR_MESSAGE);
                })
                .doOnSubscribe(sub -> log.info("Executing uploadDarFile service"))
                .doFinally(sub -> log.info("Executed uploadDarFile service"));
    }

    /**
     * If automated retries to connect to the Ledger fail then manual intervention is needed
     * After Ledger issues are resolved, this method can be called to reconnect and restart the listener
     * @return true if connected else error
     */
    @Override
    public Mono<Boolean> manualLedgerConnect() {
        return ledgerClient.isConnected()
                .flatMap(connected -> {
                    if (Boolean.TRUE.equals(connected)) {
                        if (Boolean.FALSE.equals(ledgerListener.isListening())) {
                            return Mono.just(true).doOnSuccess(sub -> restartListener());
                        }
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Ledger Client already connected.");
                    } else {
                        return LedgerHelper.connect(ledgerClient)
                                .doOnSubscribe(sub -> log.info("Executing manualLedgerConnect service"))
                                .doOnSuccess(sub -> restartListener());
                    }
                });
    }

    private void restartListener() {
        log.info("Attempting to restart the LedgerListener...");
        ledgerListener.readActiveContracts();
    }
}