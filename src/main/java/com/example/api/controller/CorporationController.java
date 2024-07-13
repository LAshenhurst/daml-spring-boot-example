package com.example.api.controller;

import com.daml.ledger.javaapi.data.Transaction;
import com.example.api.domain.Role;
import com.example.api.domain.asset.cash.CreateCashRequest;
import com.example.api.domain.exchange.Offer;
import com.example.api.domain.asset.share.CreateShareRequest;
import com.example.api.service.AuthenticationService;
import com.example.api.service.CorporationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/corporation")
@RequiredArgsConstructor
public class CorporationController {
    private final CorporationService corporationService;
    private final AuthenticationService authenticationService;

    @PostMapping(path = "/share-request")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a Share Request")
    public Mono<Transaction> createCorporationShareRequest(@Valid @RequestBody CreateShareRequest createShareRequest,
                                                            @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = corporationService.createShareRequest(createShareRequest, workflowId);
        return authenticationService.checkUserRoleAndContinue(Role.CORP, responseFlow);
    }

    @PostMapping(path = "/cash-request")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a Cash Request as a Corporation")
    public Mono<Transaction> createCorporationCashRequest(@Valid @RequestBody CreateCashRequest createCashRequest,
                                                           @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = corporationService.createCashRequest(createCashRequest, workflowId);
        return authenticationService.checkUserRoleAndContinue(Role.CORP, responseFlow);
    }

    @PostMapping(path = "/buy-offer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a Buy Offer as a corporation")
    public Mono<Transaction> createCorporationBuyOffer(@Valid @RequestBody Offer offer,
                                                        @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow =  corporationService.createBuyOffer(offer, workflowId);
        return authenticationService.checkUserRoleAndContinue(Role.CORP, responseFlow);
    }

    @PostMapping(path = "/sell-offer")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a Sell Offer as a Corporation")
    public Mono<Transaction> createCorporationSellOffer(@Valid @RequestBody Offer offer,
                                                         @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow =  corporationService.createSellOffer(offer, workflowId);
        return authenticationService.checkUserRoleAndContinue(Role.CORP, responseFlow);
    }
}
