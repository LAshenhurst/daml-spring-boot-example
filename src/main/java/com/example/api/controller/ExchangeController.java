package com.example.api.controller;

import com.daml.ledger.javaapi.data.Transaction;
import com.example.api.common.exceptions.ApiException;
import com.example.api.configuration.properties.LedgerProperties;
import com.example.api.domain.exchange.BuyOffer;
import com.example.api.domain.exchange.SellOffer;
import com.example.api.domain.rest.request.FilterRequest;
import com.example.api.domain.rest.request.SortModel;
import com.example.api.domain.rest.response.FilterResponse;
import com.example.api.helper.FilterHelper;
import com.example.api.helper.SecurityHelper;
import com.example.api.service.AuthenticationService;
import com.example.api.service.ExchangeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;

@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
public class ExchangeController {
    private final ExchangeService exchangeService;
    private final LedgerProperties ledgerProperties;
    private final AuthenticationService authenticationService;

    private static final SortModel DEFAULT_SORT = new SortModel("latestUpdateTimestamp", Sort.Direction.DESC);

    @GetMapping("/buy/{id}")
    @Operation(summary = "Get a Buy Offer by its unique identifier")
    public Mono<BuyOffer> getBuyById(@PathVariable String id) {
        return SecurityHelper.getUsername()
                .flatMap(username ->
                        exchangeService.getBuyOfferById(id)
                                .map(buyOffer -> {
                                    if (ledgerProperties.getCustodian().equalsIgnoreCase(username) || buyOffer.getBuyer().equalsIgnoreCase(username)) {
                                        return buyOffer;
                                    }
                                    throw new ApiException(HttpStatus.NOT_FOUND, "Buy Offer with id '" + id + "' not found.");
                                })
                );
    }

    @PostMapping("/buy/filters")
    @Operation(summary = "Perform queries to return Buy Offers from the Ledger")
    public Mono<FilterResponse<BuyOffer>> buyOfferFilter(@RequestBody FilterRequest filterRequest) {
        return SecurityHelper.getUsername()
                .flatMap(username -> {
                    if (filterRequest.getSortModel() == null || filterRequest.getSortModel().isEmpty()) {
                        filterRequest.setSortModel(Collections.singletonList(DEFAULT_SORT));
                    }
                    Criteria filterCriteria = FilterHelper.newCriteria(filterRequest, BuyOffer.class);
                    Criteria buyerCriteria = ledgerProperties.getCustodian().equalsIgnoreCase(username) ?
                            Criteria.empty() : Criteria.where("buyer").is(username);
                    Pageable pageable = FilterHelper.newPageRequest(filterRequest, BuyOffer.class);

                    return exchangeService.buyOfferFilter(Criteria.from(filterCriteria, buyerCriteria), pageable)
                            .collectList()
                            .flatMap(buyOffers -> exchangeService.buyOfferFilterCount(Criteria.from(filterCriteria, buyerCriteria))
                                    .map(totalRows -> new FilterResponse<>(buyOffers, totalRows))
                            );
                });
    }

    @GetMapping("/sell/{id}")
    @Operation(summary = "Get a Sell Offer by its unique identifier")
    public Mono<SellOffer> getSellById(@PathVariable String id) {
        return SecurityHelper.getUsername()
                .flatMap(username ->
                        exchangeService.getSellOfferById(id)
                                .map(sellOffer -> {
                                    if (ledgerProperties.getCustodian().equalsIgnoreCase(username) || sellOffer.getSeller().equalsIgnoreCase(username)) {
                                        return sellOffer;
                                    }
                                    throw new ApiException(HttpStatus.NOT_FOUND, "Sell Offer with id '" + id + "' not found.");
                            })
                );
    }

    @PostMapping("/sell/filters")
    @Operation(summary = "Perform queries to get Sell Offers from the Ledger")
    public Mono<FilterResponse<SellOffer>> sellOfferFilter(@RequestBody FilterRequest filterRequest) {
        return SecurityHelper.getUsername()
                .flatMap(username -> {
                    if (filterRequest.getSortModel() == null || filterRequest.getSortModel().isEmpty()) {
                        filterRequest.setSortModel(Collections.singletonList(DEFAULT_SORT));
                    }
                    Criteria filterCriteria = FilterHelper.newCriteria(filterRequest, SellOffer.class);
                    Criteria sellerCriteria = ledgerProperties.getCustodian().equalsIgnoreCase(username) ?
                            Criteria.empty() : Criteria.where("seller").is(username);
                    Pageable pageable = FilterHelper.newPageRequest(filterRequest, SellOffer.class);

                    return exchangeService.sellOfferFilter(Criteria.from(filterCriteria, sellerCriteria), pageable)
                            .collectList()
                            .flatMap(sellOffers -> exchangeService.sellOfferFilterCount(Criteria.from(filterCriteria, sellerCriteria))
                                    .map(totalRows -> new FilterResponse<>(sellOffers, totalRows))
                            );
                });
    }

    @DeleteMapping("/sell/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Cancel an existing Sell Offer")
    public Mono<Transaction> cancelSellOffer(@PathVariable String id,
                                             @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        return exchangeService.cancelSellOffer(id, workflowId);
    }

    @DeleteMapping("/buy/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Cancel an existing Buy Offer")
    public Mono<Transaction> cancelBuyOffer(@PathVariable String id,
                                            @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        return exchangeService.cancelBuyOffer(id, workflowId);
    }
}