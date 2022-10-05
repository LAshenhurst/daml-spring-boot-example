package com.lashe.example.api.controller;

import com.daml.ledger.javaapi.data.Transaction;
import com.lashe.example.api.common.exceptions.ApiException;
import com.lashe.example.api.configuration.properties.LedgerProperties;
import com.lashe.example.api.domain.asset.AssetRequestResponse;
import com.lashe.example.api.domain.asset.ChangeAssetState;
import com.gft.example.api.domain.asset.share.*;
import com.lashe.example.api.domain.asset.share.Share;
import com.lashe.example.api.domain.asset.share.ShareRequest;
import com.lashe.example.api.domain.rest.request.FilterRequest;
import com.lashe.example.api.domain.rest.request.SortModel;
import com.lashe.example.api.domain.rest.response.FilterResponse;
import com.lashe.example.api.helper.FilterHelper;
import com.lashe.example.api.helper.SecurityHelper;
import com.lashe.example.api.service.AuthenticationService;
import com.lashe.example.api.service.ShareService;
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
@RequestMapping("/shares")
@RequiredArgsConstructor
public class ShareController {
    private final ShareService shareService;
    private final LedgerProperties ledgerProperties;
    private final AuthenticationService authenticationService;

    private static final SortModel DEFAULT_SORT = new SortModel("latestUpdateTimestamp", Sort.Direction.DESC);

    @GetMapping("/{id}")
    @Operation(summary = "Get a Share by its unique identifier")
    public Mono<Share> getShareById(@PathVariable String id) {
        return shareService.getSharesById(id)
                .flatMap(share ->
                        SecurityHelper.getUsername()
                                .map(username -> {
                                    if (share.getOwner().equalsIgnoreCase(username) || ledgerProperties.getCustodian().equalsIgnoreCase(username)) {
                                        return share;
                                    }
                                    throw new ApiException(HttpStatus.NOT_FOUND, "Share with id '" + id + "' not found.");
                                })
                );
    }

    @PostMapping("/filters")
    @Operation(summary = "Perform queries to return Share objects from the ledger")
    public Mono<FilterResponse<Share>> shareFilter(@RequestBody FilterRequest filterRequest) {
        return SecurityHelper.getUsername()
                .flatMap(username -> {
                    if (filterRequest.getSortModel() == null || filterRequest.getSortModel().isEmpty()) {
                        filterRequest.setSortModel(Collections.singletonList(DEFAULT_SORT));
                    }
                    Criteria filterCriteria = FilterHelper.newCriteria(filterRequest, Share.class);
                    Criteria ownerCriteria = ledgerProperties.getCustodian().equalsIgnoreCase(username) ?
                            Criteria.empty() : Criteria.where("owner").is(username);
                    Pageable pageable = FilterHelper.newPageRequest(filterRequest, Share.class);

                    return shareService.shareFilter(Criteria.from(filterCriteria, ownerCriteria), pageable)
                            .collectList()
                            .flatMap(shareData -> shareService.shareFilterCount(Criteria.from(filterCriteria, ownerCriteria))
                                    .map(totalRows -> new FilterResponse<>(shareData, totalRows))
                            );
                });
    }

    @PostMapping("/requests/filters")
    @Operation(summary = "Perform queries to return ShareRequest objects from the ledger")
    public Mono<FilterResponse<ShareRequest>> shareRequestFilter(@RequestBody FilterRequest filterRequest) {
        return SecurityHelper.getUsername()
                .flatMap(username -> {
                    Criteria filterCriteria = FilterHelper.newCriteria(filterRequest, ShareRequest.class);
                    Criteria requesterCriteria = ledgerProperties.getCustodian().equalsIgnoreCase(username) ?
                            Criteria.empty() : Criteria.where("requester").is(username);
                    Pageable pageable = FilterHelper.newPageRequest(filterRequest, ShareRequest.class);

                    return shareService.shareRequestFilter(Criteria.from(filterCriteria, requesterCriteria), pageable)
                            .collectList()
                            .flatMap(shareData -> shareService.shareRequestFilterCount(Criteria.from(filterCriteria, requesterCriteria))
                                    .map(totalRows -> new FilterResponse<>(shareData, totalRows))
                            );
                });
    }

    @PostMapping(path = "/requests/accept")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Allows the Exchange owner to accept a Share Request")
    public Mono<Transaction> updateShareRequestAccept(@Valid @RequestBody AssetRequestResponse assetRequestResponse,
                                                       @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow =  shareService.updateShareRequestAccept(assetRequestResponse, workflowId);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }

    @PostMapping(path = "/requests/decline")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Allows the Exchange owner to decline a Share Request")
    public Mono<Transaction> updateShareRequestDecline(@Valid @RequestBody AssetRequestResponse assetRequestResponse,
                                                        @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = shareService.updateShareRequestDecline(assetRequestResponse, workflowId);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }

    @PostMapping(path = "/change-state")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Allows the Exchange owner to change the state of an existing Share instance")
    public Mono<Transaction> deleteShares(@Valid @RequestBody ChangeAssetState changeAssetState,
                                           @RequestHeader(required = false, value = "Workflow-Id") String workflowId) {
        Mono<Transaction> responseFlow = shareService.changeShareState(changeAssetState, workflowId);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }
}