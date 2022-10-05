package com.lashe.example.api.service;

import com.lashe.example.api.domain.daml.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionService {
    Mono<Transaction> getTransactionById(String transactionId);

    Flux<Transaction> getTransactions(String beginOffset, String endOffset);
}