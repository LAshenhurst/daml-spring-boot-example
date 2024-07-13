package com.example.api.controller;

import com.example.api.domain.daml.Transaction;
import com.example.api.service.AuthenticationService;
import com.example.api.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final AuthenticationService authenticationService;

    @GetMapping("/{id}")
    @Operation(summary = "Get a Ledger transaction by id")
    public Mono<Transaction> getTransactionById(@PathVariable String id) {
        Mono<Transaction> responseFlow = transactionService.getTransactionById(id);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }

    @GetMapping
    @Operation(summary = "Get Ledger transactions between two offsets, ledger start/end will be used if values not provided.")
    public Flux<Transaction> getTransactions(@RequestParam(value = "startOffset", required = false) String startOffset,
                                             @RequestParam(value = "endOffset", required = false) String endOffset) {
        Flux<Transaction> responseFlow = transactionService.getTransactions(startOffset, endOffset);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }
}
