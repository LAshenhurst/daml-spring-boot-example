package com.example.api.controller;

import com.daml.ledger.javaapi.data.Transaction;
import com.example.api.domain.CreateCorporation;
import com.example.api.domain.CreateTrader;
import com.example.api.domain.exchange.MatchOffers;
import com.example.api.service.AuthenticationService;
import com.example.api.service.CustodianService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/custodian")
@RequiredArgsConstructor
public class CustodianController {
    private final CustodianService custodianService;
    private final AuthenticationService authenticationService;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Allows the Exchange owner to create an instance of Custodian on the Ledger")
    public Mono<Transaction> createCustodian(@RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = custodianService.createCustodian(workflowId);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }

    @PostMapping(path = "/corporation")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Allows the Exchange owner to create a Corporation on the Ledger")
    public Mono<Transaction> createCorporation(@Valid @RequestBody CreateCorporation createCorporation,
                                                @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow =  custodianService.createCorporation(createCorporation, workflowId);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }

    @PostMapping(path = "/trader")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Allows the Exchange owner to create a Trader on the Ledger")
    public Mono<Transaction> createTrader(@Valid @RequestBody CreateTrader createTrader,
                                           @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = custodianService.createTrader(createTrader, workflowId);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }

    @PostMapping(path = "/match")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Allows the Exchange owner to match a Buy Offer and Sell Offer")
    public Mono<Transaction> exerciseMatch(@Valid @RequestBody MatchOffers match,
                                            @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = custodianService.exerciseMatch(match, workflowId);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }
}
