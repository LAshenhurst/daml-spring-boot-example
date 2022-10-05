package com.lashe.example.api.common.daml;

import com.daml.ledger.javaapi.data.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Adapter to deal with Ledger client calls. Providing common error logic
 */
public interface LedgerClientAdapter {
    Mono<Transaction> submitCommand(String workflowId, Command command);

    Mono<Transaction> submitCommands(String workflowId, List<Command> commandList);

    Flux<GetActiveContractsResponse> read(String party, Set<Identifier> identifiers);

    Mono<Transaction> getTransactionById(String party, String transactionId);

    Flux<Transaction> getTransactions(LedgerOffset begin, LedgerOffset end, String party, Set<Identifier> identifiers);

    Flux<Transaction> listen(LedgerOffset begin, String party, Set<Identifier> identifiers);
}
