package com.lashe.example.api.service.impl;

import com.daml.ledger.javaapi.data.LedgerOffset;
import com.lashe.example.api.common.AppConstants;
import com.lashe.example.api.common.daml.LedgerClientAdapter;
import com.lashe.example.api.common.exceptions.ApiException;
import com.lashe.example.api.configuration.properties.LedgerProperties;
import com.lashe.example.api.domain.daml.Transaction;
import com.lashe.example.api.domain.mapper.LedgerMapper;
import com.lashe.example.api.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final LedgerMapper ledgerMapper;
    private final LedgerProperties ledgerProperties;
    private final LedgerClientAdapter ledgerClientAdapter;

    @Override
    public Mono<Transaction> getTransactionById(String transactionId) {
        return ledgerClientAdapter.getTransactionById(ledgerProperties.getCustodianPartyId(), transactionId)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "Transaction not found.")))
                .map(ledgerMapper::toDomain)
                .doOnSubscribe(sub -> log.info("Executing getTransactionById service"))
                .doFinally(sub -> log.info("Executed getTransactionById service"));
    }

    @Override
    public Flux<Transaction> getTransactions(String beginOffset, String endOffset) {
        LedgerOffset start = StringUtils.isEmpty(beginOffset) ? LedgerOffset.LedgerBegin.getInstance() : new LedgerOffset.Absolute(beginOffset);
        LedgerOffset end = StringUtils.isEmpty(endOffset) ? LedgerOffset.LedgerEnd.getInstance() : new LedgerOffset.Absolute(endOffset);
        return ledgerClientAdapter.getTransactions(start, end, ledgerProperties.getCustodianPartyId(), Collections.emptySet())
                .map(ledgerMapper::toDomain)
                .doOnError(ex -> {
                    if (ex.getMessage().contains("INVALID_ARGUMENT")) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid Offset(s) provided.");
                    }
                    log.warn("Unexpected getTransactions error", ex);
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, AppConstants.DEFAULT_ERROR_MESSAGE);
                })
                .doOnSubscribe(sub -> log.info("Executing getTransactions service"))
                .doFinally(sub -> log.info("Executed getTransactions service"));
    }
}