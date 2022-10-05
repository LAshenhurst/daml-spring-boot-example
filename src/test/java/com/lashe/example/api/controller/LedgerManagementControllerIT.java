package com.lashe.example.api.controller;

import com.lashe.example.api.config.WebFluxTestConfig;
import com.lashe.example.api.domain.daml.CreateLedgerParty;
import com.lashe.example.api.domain.daml.LedgerParty;
import com.lashe.example.api.domain.daml.PackageDetails;
import com.google.common.net.HttpHeaders;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.List;

@ExtendWith(SpringExtension.class)
@Import(WebFluxTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LedgerManagementControllerIT {
    @Autowired
    private WebTestClient exchangeWebClient;

    @Autowired
    private WebTestClient traderWebClient;

    private static final String LEDGER_MANAGEMENT_URI = "/ledger-management";
    private static final String LEDGER_CONNECT_URI = LEDGER_MANAGEMENT_URI + "/connect";
    private static final String LEDGER_PARTIES_URI = LEDGER_MANAGEMENT_URI + "/parties";
    private static final String PRUNE_LEDGER_URI = LEDGER_MANAGEMENT_URI + "/prune";
    private static final String LEDGER_PACKAGES_URI = LEDGER_MANAGEMENT_URI + "/packages";

    @Test
    @Order(1)
    @DisplayName("LedgerConnect_AlreadyConnected")
    void Given_LedgerAlreadyConnected_When_AttemptingLedgerReconnect_Then_ErrorReturned() {
        exchangeWebClient.post()
                .uri(LEDGER_CONNECT_URI)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(2)
    @DisplayName("LedgerConnect_PermissionDenied")
    void Given_TraderUser_When_AttemptingLedgerReconnect_Then_PermissionDenied() {
        traderWebClient.post()
                .uri(LEDGER_CONNECT_URI)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @Order(3)
    @DisplayName("GetLegerParties_HappyPath")
    void Given_AdminUser_When_GettingLedgerParties_Then_PartiesReturned() {
        List<LedgerParty> parties = exchangeWebClient.get()
                .uri(LEDGER_PARTIES_URI)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(LedgerParty.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(parties);
        Assertions.assertFalse(parties.isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("GetLedgerPartyById_HappyPath")
    void Given_ValidLedgerPartyId_When_GettingLedgerPartyById_Then_LedgerPartyReturned() {
        List<LedgerParty> parties = exchangeWebClient.get()
                .uri(LEDGER_PARTIES_URI)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(LedgerParty.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(parties);
        Assertions.assertFalse(parties.isEmpty());

        LedgerParty party = exchangeWebClient.get()
                .uri(LEDGER_PARTIES_URI + "/" + parties.get(0).getIdentifier())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(LedgerParty.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(party);
        Assertions.assertEquals(parties.get(0), party);
    }

    @Test
    @Order(5)
    @DisplayName("GetLedgerPartyById_BadId")
    void Given_BadIdentifier_When_GettingLedgerPartyById_Then_404Returned() {
        exchangeWebClient.get()
                .uri(LEDGER_PARTIES_URI + "/" + RandomStringUtils.randomAlphanumeric(8))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(6)
    @DisplayName("CreateLedgerParty_HappyPath")
    void Given_ValidPartyDetails_When_CreatingNewLedgerParty_Then_PartyCreated() {
        String party = RandomStringUtils.randomAlphanumeric(12);
        CreateLedgerParty createLedgerParty = CreateLedgerParty.builder()
                .identifierHint(party)
                .displayName(party)
                .build();

        LedgerParty newParty = exchangeWebClient.post()
                .uri(LEDGER_PARTIES_URI)
                .body(BodyInserters.fromValue(createLedgerParty))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(LedgerParty.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(newParty);
        Assertions.assertEquals(party, newParty.getDisplayName());
    }

    @Test
    @Order(7)
    @DisplayName("CreateLedgerParty_BadIdentifier")
    void Given_InvalidPartyDetails_When_CreatingLedgerParty_Then_400ErrorReturned() {
        String party = RandomStringUtils.randomAlphanumeric(12) + "$$%$%";
        CreateLedgerParty createLedgerParty = CreateLedgerParty.builder()
                .identifierHint(party)
                .displayName(party)
                .build();

        exchangeWebClient.post()
                .uri(LEDGER_PARTIES_URI)
                .body(BodyInserters.fromValue(createLedgerParty))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(8)
    @DisplayName("CreateLedgerParty_AlreadyExists")
    void Given_LedgerPartyAlreadyExists_When_CreatingLedgerParty_Then_400ErrorReturned() {
        String party = RandomStringUtils.randomAlphanumeric(12);
        CreateLedgerParty createLedgerParty = CreateLedgerParty.builder()
                .identifierHint(party)
                .displayName(party)
                .build();

        LedgerParty newParty = exchangeWebClient.post()
                .uri(LEDGER_PARTIES_URI)
                .body(BodyInserters.fromValue(createLedgerParty))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(LedgerParty.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(newParty);

        exchangeWebClient.post()
                .uri(LEDGER_PARTIES_URI)
                .body(BodyInserters.fromValue(createLedgerParty))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(9)
    @DisplayName("CreateLedgerParty_PermissionDenied")
    void Given_TraderUser_When_CreatingNewLedgerParty_Then_PermissionDenied() {
        String party = RandomStringUtils.randomAlphanumeric(12);
        CreateLedgerParty createLedgerParty = CreateLedgerParty.builder()
                .identifierHint(party)
                .displayName(party)
                .build();

        traderWebClient.post()
                .uri(LEDGER_PARTIES_URI)
                .body(BodyInserters.fromValue(createLedgerParty))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @Order(10)
    @DisplayName("PruneLedger_PermissionDenied")
    void Given_TraderUser_When_PruningLedger_Then_PermissionDenied() {
        traderWebClient.post()
                .uri(PRUNE_LEDGER_URI + "/" + RandomStringUtils.randomAlphanumeric(12))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @Order(11)
    @DisplayName("PruneLedger_BadOffset")
    void Given_InvalidLedgerOffset_When_PruningLedger_Then_400ErrorReturned() {
        exchangeWebClient.post()
                .uri(PRUNE_LEDGER_URI + "/" + RandomStringUtils.randomAlphanumeric(12))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(12)
    @DisplayName("GetLedgerPackages_HappyPath")
    void Given_AdminUser_When_GettingLedgerPackages_Then_PackagesReturned() {
        List<PackageDetails> packages = exchangeWebClient.get()
                .uri(LEDGER_PACKAGES_URI)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(PackageDetails.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(packages);
        Assertions.assertFalse(packages.isEmpty());
    }

    @Test
    @Order(13)
    @DisplayName("GetLedgerPackageById_HappyPath")
    void Given_ValidPackageId_When_GettingLedgerPackageById_Then_PackageReturned() {
        List<PackageDetails> packages = exchangeWebClient.get()
                .uri(LEDGER_PACKAGES_URI)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(PackageDetails.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(packages);
        Assertions.assertFalse(packages.isEmpty());

        PackageDetails packageDetails = exchangeWebClient.get()
                .uri(LEDGER_PACKAGES_URI + "/" + packages.get(0).getIdentifier())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(PackageDetails.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(packageDetails);
        Assertions.assertEquals(packages.get(0), packageDetails);
    }

    @Test
    @Order(14)
    @DisplayName("GetLedgerPackageById_BadId")
    void Given_BadIdentifier_When_GettingLedgerPackageById_Then_404Returned() {
        exchangeWebClient.get()
                .uri(LEDGER_PACKAGES_URI + "/" + RandomStringUtils.randomAlphanumeric(12))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(15)
    @DisplayName("AddLedgerPackage_HappyPath")
    void Given_ValidLedgerPackage_When_AddingLedgerPackage_Then_PackageUploaded() {
        final Resource exampleDar = new ClassPathResource("create-daml-app-0.1.0.dar");
        exchangeWebClient.post()
                .uri(LEDGER_PACKAGES_URI)
                .body(BodyInserters.fromMultipartData("file", exampleDar))
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(16)
    @DisplayName("AddLedgerPackage_PermissionDenied")
    void Given_TraderUser_When_AddingLedgerPackage_Then_PermissionDenied() {
        final Resource exampleDar = new ClassPathResource("create-daml-app-0.1.0.dar");
        traderWebClient.post()
                .uri(LEDGER_PACKAGES_URI)
                .body(BodyInserters.fromMultipartData("file", exampleDar))
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @Order(17)
    @DisplayName("AddLedgerPackage_NoPackage")
    void Given_NoPackageProvided_When_AddingLedgerPackage_Then_400ErrorReturned() {
        exchangeWebClient.post()
                .uri(LEDGER_PACKAGES_URI)
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .exchange()
                .expectStatus().is4xxClientError();
    }
}