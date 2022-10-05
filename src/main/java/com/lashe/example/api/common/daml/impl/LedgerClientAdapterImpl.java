package com.lashe.example.api.common.daml.impl;

import com.daml.ledger.javaapi.data.*;
import com.lashe.example.api.common.exceptions.ApiException;
import com.lashe.example.api.common.daml.LedgerClient;
import com.lashe.example.api.common.daml.LedgerClientAdapter;
import com.lashe.example.api.configuration.properties.LedgerProperties;
import com.lashe.example.api.configuration.properties.ServerProperties;
import com.lashe.example.api.helper.LedgerHelper;
import com.lashe.example.api.helper.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter to deal with Ledger client calls. Providing common error logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerClientAdapterImpl implements LedgerClientAdapter {

    private final LedgerClient ledgerClient;
    private final LedgerProperties ledgerProperties;
    private final ServerProperties serverProperties;

    public Flux<GetActiveContractsResponse> read(String party, Set<Identifier> identifiers) {
        return RxJava2Adapter.flowableToFlux(ledgerClient.getActiveContractSetClient()
                .getActiveContracts(buildFilter(party, identifiers), true))
                .doOnSubscribe(sub -> log.debug("Reading Ledger using Identifiers {} for Party {}", identifiers, party))
                .onErrorMap(ex -> handleLedgerException(ex, "readCommand with identifiers: " + identifiers));
    }

    public Flux<Transaction> listen(LedgerOffset begin, String party, Set<Identifier> identifiers) {
        return RxJava2Adapter.flowableToFlux(ledgerClient.getTransactionsClient().getTransactions(begin, buildFilter(party, identifiers), true));
    }

    public Mono<Transaction> getTransactionById(String party, String transactionId) {
        return RxJava2Adapter.singleToMono(ledgerClient.getTransactionsClient().getFlatTransactionById(transactionId, Collections.singleton(party)))
                .onErrorMap(ex -> handleLedgerException(ex, "getTransactionCommand with transactionId: " + transactionId));
    }

    public Flux<Transaction> getTransactions(LedgerOffset begin, LedgerOffset end, String party, Set<Identifier> identifiers) {
        return RxJava2Adapter.flowableToFlux(ledgerClient.getTransactionsClient().getTransactions(begin, end, buildFilter(party, identifiers), true))
                .onErrorMap(ex -> handleLedgerException(ex, "getTransactions with begin: " + begin + " end: " + end));
    }

    private FiltersByParty buildFilter(String party, Set<Identifier> identifiers) {
        Filter filter = !identifiers.isEmpty() ? new InclusiveFilter(identifiers) : NoFilter.instance;
        return new FiltersByParty(Collections.singletonMap(party, filter));
    }

    public Mono<Transaction> submitCommand(String workflowId, Command command) {
        return submitCommands(workflowId, Collections.singletonList(command));
    }

    public Mono<Transaction> submitCommands(String workflowId, List<Command> commandList) {
        return SecurityHelper.getUsername()
                .map(ledgerProperties::getLedgerIdFromDisplayName)
                .flatMap(partyId -> {
                    String commandId = UUID.randomUUID().toString();

                    return RxJava2Adapter
                            .singleToMono(ledgerClient.getCommandClient().submitAndWaitForTransaction(
                                    StringUtils.isEmpty(workflowId) ? UUID.randomUUID().toString() : workflowId,
                                    serverProperties.getAppId(),
                                    commandId,
                                    partyId,
                                    commandList))
                            .doOnSubscribe(subscription -> log.debug("Command " + commandId + " subscribed"))
                            .doOnSuccess(transaction -> log.debug("Command " + commandId + " executed"))
                            .onErrorMap(exception -> handleLedgerException(exception, commandId));
                });
    }

    private static Throwable handleLedgerException(Throwable ex, String commandId) {
        log.warn("Command with id '{}' failed: {}", commandId, ex.getMessage());
        if (ex.getMessage().contains("ALREADY_EXISTS")) {
            return new ApiException(HttpStatus.BAD_REQUEST, "Object already exists on the Ledger.");
        } else if (ex.getMessage().contains("NOT_FOUND")) {
            return new ApiException(HttpStatus.NOT_FOUND, LedgerHelper.extractErrorMessage(ex.getMessage()));
        }

        return new ApiException(HttpStatus.BAD_REQUEST, LedgerHelper.extractErrorCode(ex.getMessage()), LedgerHelper.extractErrorMessage(ex.getMessage()));
    }
}
