package com.lashe.example.api.controller;

import com.daml.ledger.javaapi.data.Transaction;
import com.lashe.example.api.common.exceptions.ApiException;
import com.lashe.example.api.configuration.properties.LedgerProperties;
import com.lashe.example.api.domain.asset.AssetRequestResponse;
import com.lashe.example.api.domain.asset.ChangeAssetState;
import com.gft.example.api.domain.asset.cash.*;
import com.lashe.example.api.domain.asset.cash.Cash;
import com.lashe.example.api.domain.asset.cash.CashRequest;
import com.lashe.example.api.domain.rest.request.FilterRequest;
import com.lashe.example.api.domain.rest.request.SortModel;
import com.lashe.example.api.domain.rest.response.FilterResponse;
import com.lashe.example.api.helper.FilterHelper;
import com.lashe.example.api.helper.SecurityHelper;
import com.lashe.example.api.service.AuthenticationService;
import com.lashe.example.api.service.CashService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Collections;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {
    private final CashService cashService;
    private final LedgerProperties ledgerProperties;
    private final AuthenticationService authenticationService;

    private static final SortModel DEFAULT_SORT = new SortModel("latestUpdateTimestamp", Sort.Direction.DESC);

    @GetMapping("/{id}")
    @Operation(summary = "Get a Cash instance by its unique identifier")
    public Mono<Cash> getCashById(@PathVariable String id) {
        return cashService.getCashById(id)
                .flatMap(cash ->
                        SecurityHelper.getUsername()
                                .map(username -> {
                                    if (cash.getOwner().equalsIgnoreCase(username) || ledgerProperties.getCustodian().equalsIgnoreCase(username)) {
                                        return cash;
                                    }
                                    throw new ApiException(HttpStatus.NOT_FOUND, "Cash with id '" + id + "' not found.");
                                })
                );
    }

    @PostMapping("/filters")
    @Operation(summary = "Perform queries to return Cash objects from the Ledger")
    public Mono<FilterResponse<Cash>> cashFilters(@RequestBody FilterRequest filterRequest) {
        return SecurityHelper.getUsername()
                .flatMap(username -> {
                    if (filterRequest.getSortModel() == null || filterRequest.getSortModel().isEmpty()) {
                        filterRequest.setSortModel(Collections.singletonList(DEFAULT_SORT));
                    }
                    Criteria filterCriteria = FilterHelper.newCriteria(filterRequest, Cash.class);
                    Criteria ownerCriteria = ledgerProperties.getCustodian().equalsIgnoreCase(username) ?
                            Criteria.empty() : Criteria.where("owner").is(username);
                    Pageable pageable = FilterHelper.newPageRequest(filterRequest, Cash.class);

                    return cashService.cashFilter(Criteria.from(filterCriteria, ownerCriteria), pageable)
                            .collectList()
                            .flatMap(cashData -> cashService.cashFilterCount(Criteria.from(filterCriteria, ownerCriteria))
                                    .map(totalRows -> new FilterResponse<>(cashData, totalRows))
                            );
                });
    }

    @PostMapping("/requests/filters")
    @Operation(summary = "Perform queries to return CashRequest objects from the ledger")
    public Mono<FilterResponse<CashRequest>> cashRequestFilters(@RequestBody FilterRequest filterRequest) {
        return SecurityHelper.getUsername()
                .flatMap(username -> {
                    Criteria filterCriteria = FilterHelper.newCriteria(filterRequest, CashRequest.class);
                    Criteria requesterCriteria = ledgerProperties.getCustodian().equalsIgnoreCase(username) ?
                            Criteria.empty() : Criteria.where("requester").is(username);
                    Pageable pageable = FilterHelper.newPageRequest(filterRequest, CashRequest.class);

                    return cashService.cashRequestFilter(Criteria.from(filterCriteria, requesterCriteria), pageable)
                            .collectList()
                            .flatMap(cashData -> cashService.cashRequestFilterCount(Criteria.from(filterCriteria, requesterCriteria))
                                    .map(totalRows -> new FilterResponse<>(cashData, totalRows))
                            );
                });
    }

    @PostMapping(path = "/requests/accept")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Allows the Exchange owner to accept a Cash Request")
    public Mono<Transaction> updateCashRequestAccept(@Valid @RequestBody AssetRequestResponse assetRequestResponse,
                                                      @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = cashService.updateCashRequestAccept(assetRequestResponse, workflowId);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }

    @PostMapping(path = "/requests/decline")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Allows the Exchange owner to decline a Cash Request")
    public Mono<Transaction> updateCashRequestDecline(@Valid @RequestBody AssetRequestResponse assetRequestResponse,
                                                       @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = cashService.updateCashRequestDecline(assetRequestResponse, workflowId);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }

    @PostMapping(path = "/change-state")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Allows the Exchange owner to change the state of an existing Cash instance")
    public Mono<Transaction> changeCashState(@Valid @RequestBody ChangeAssetState changeAssetState,
                                         @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = cashService.changeCashState(changeAssetState, workflowId);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }
}
