package com.example.api.controller;

import com.example.api.common.exceptions.ApiException;
import com.example.api.domain.daml.CreateLedgerParty;
import com.example.api.domain.daml.LedgerParty;
import com.example.api.domain.daml.PackageDetails;
import com.example.api.service.AuthenticationService;
import com.example.api.service.LedgerManagementService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ledger-management")
public class LedgerManagementController {
    private final LedgerManagementService ledgerManagementService;
    private final AuthenticationService authenticationService;

    @PostMapping("/connect")
    @Operation(summary = "Attempt to connect the Ledger Client to the Ledger if not already connected.")
    public <T> Mono<ResponseEntity<T>> connectLedgerClient() {
        Mono<ResponseEntity<T>> responseFlow =  ledgerManagementService.manualLedgerConnect()
                .map(connected -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }

    @GetMapping("/parties")
    @Operation(summary = "List all parties on the Ledger.")
    public Flux<LedgerParty> getLedgerParties() { return ledgerManagementService.getLedgerParties(); }

    @PostMapping("/parties")
    @Operation(summary = "Add a new Ledger Party.")
    public Mono<LedgerParty> addLedgerParty(@RequestBody CreateLedgerParty createLedgerParty) {
        Mono<LedgerParty> responseFlow =  ledgerManagementService.createLedgerParty(createLedgerParty);
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }

    @GetMapping("/parties/{id}")
    @Operation(summary = "Get Party by unique DAML identifier")
    public Mono<LedgerParty> getPartyByLedgerId(@PathVariable String id) {
        return ledgerManagementService.getLedgerPartyById(id);
    }

    @PostMapping("/prune/{pruneOffset}")
    @Operation(summary = "Prune the Ledger at a given Offset.")
    public Mono<ResponseEntity<String>> pruneLedger(@PathVariable String pruneOffset) {
        Mono<ResponseEntity<String>> responseFlow =  ledgerManagementService.pruneLedger(pruneOffset)
                .map(resource -> new ResponseEntity<>(resource.getResourceId(), HttpStatus.OK));
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }

    @GetMapping("/packages")
    @Operation(summary = "List all DAML Packages installed at the Ledger.")
    public Flux<PackageDetails> getLedgerPackages() { return ledgerManagementService.getPackages(); }

    @GetMapping("/packages/{id}")
    @Operation(summary = "Find a DAML Package on the Ledger by id.")
    public Mono<PackageDetails> getLedgerPackage(@PathVariable String id) { return ledgerManagementService.getPackageById(id); }

    @PostMapping("/packages")
    @Operation(summary = "Upload a new DAR file package to the Ledger.")
    public Mono<ResponseEntity<String>> uploadDarFile(@RequestPart("file") FilePart filePart) {
        Mono<ResponseEntity<String>> responseFlow =  DataBufferUtils.join(filePart.content())
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.BAD_REQUEST, "Invalid or Empty file.")))
                .flatMap(ledgerManagementService::uploadDarFile)
                .map(resource -> new ResponseEntity<>(resource.getResourceId(), HttpStatus.OK));
        return authenticationService.checkAdminPermissionAndContinue(responseFlow);
    }
}
