package com.lashe.example.api.controller;

import com.daml.ledger.javaapi.data.Transaction;
import com.lashe.example.api.domain.Role;
import com.lashe.example.api.domain.asset.cash.CreateCashRequest;
import com.lashe.example.api.domain.exchange.Offer;
import com.lashe.example.api.service.AuthenticationService;
import com.lashe.example.api.service.TraderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/trader")
@RequiredArgsConstructor
public class TraderController {
    private final TraderService traderService;
    private final AuthenticationService authenticationService;

    @PostMapping("/cash-request")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a Cash Request as a Trader")
    public Mono<Transaction> createTraderCashRequest(@Valid @RequestBody CreateCashRequest createCashRequest,
                                                      @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = traderService.createCashRequest(createCashRequest, workflowId);
        return authenticationService.checkUserRoleAndContinue(Role.TRDR, responseFlow);
    }

    @PostMapping("/buy-offer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a Buy Offer as a Trader")
    public Mono<Transaction> createTraderBuyOffer(@Valid @RequestBody Offer offer,
                                                   @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = traderService.createBuyOffer(offer, workflowId);
        return authenticationService.checkUserRoleAndContinue(Role.TRDR, responseFlow);
    }

    @PostMapping("/sell-offer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a Sell Offer as a Trader")
    public Mono<Transaction> createTraderSellOffer(@Valid @RequestBody Offer offer,
                                                    @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = traderService.createSellOffer(offer, workflowId);
        return authenticationService.checkUserRoleAndContinue(Role.TRDR, responseFlow);
    }
}