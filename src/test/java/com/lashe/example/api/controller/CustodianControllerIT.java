package com.lashe.example.api.controller;

import com.lashe.example.api.common.filters.ColumnFilter;
import com.lashe.example.api.common.filters.impl.NumberColumnFilter;
import com.lashe.example.api.common.filters.impl.TextColumnFilter;
import com.lashe.example.api.config.TestUtils;
import com.lashe.example.api.config.WebFluxTestConfig;
import com.lashe.example.api.domain.CreateCorporation;
import com.lashe.example.api.domain.CreateTrader;
import com.lashe.example.api.domain.Role;
import com.lashe.example.api.domain.asset.cash.CreateCashRequest;
import com.lashe.example.api.domain.asset.share.CreateShareRequest;
import com.lashe.example.api.domain.exchange.BuyOffer;
import com.lashe.example.api.domain.exchange.MatchOffers;
import com.lashe.example.api.domain.exchange.Offer;
import com.lashe.example.api.domain.exchange.SellOffer;
import com.lashe.example.api.domain.rest.request.FilterRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@Import(WebFluxTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CustodianControllerIT {
    @Autowired
    private WebTestClient traderWebClient;

    @Autowired
    private WebTestClient exchangeWebClient;

    @Autowired
    private WebTestClient corporationWebClient;

    private static final String CUSTODIAN_URI = "/custodian";
    private static final String CUSTODIAN_CORP_URI = CUSTODIAN_URI + "/corporation";
    private static final String CUSTODIAN_TRADER_URI = CUSTODIAN_URI + "/trader";
    private static final String CUSTODIAN_MATCH_URI = CUSTODIAN_URI + "/match";

    @Test
    @Order(1)
    @DisplayName("CreateCustodian_UnhappyPath")
    void Given_CustodianExists_When_CreatingCustodian_Then_ErrorReturned() {
        exchangeWebClient.post()
                .uri(CUSTODIAN_URI)
                .header("Workflow-Id", "IT-createCustodian")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(2)
    @DisplayName("CreateCorporation_HappyPath")
    void Given_PartyExists_When_CreatingCorporation_Then_CorporationCreated() {
        String testCorpPartyName = "TestCorp-" + RandomStringUtils.randomAlphanumeric(8);
        TestUtils.createParty(exchangeWebClient, testCorpPartyName);

        CreateCorporation createCorporation = CreateCorporation.builder()
                .corporation(testCorpPartyName)
                .build();

        exchangeWebClient.post()
                .uri(CUSTODIAN_CORP_URI)
                .header("Workflow-Id", "IT-createCorporation")
                .body(BodyInserters.fromValue(createCorporation))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(3)
    @DisplayName("CreateCorporation_NoLedgerParty")
    void Given_NoPartyExists_When_CreatingCorporation_Then_ErrorReturned() {
        String testCorpPartyName = "TestCorp-" + RandomStringUtils.randomAlphanumeric(8);
        CreateCorporation createCorporation = CreateCorporation.builder()
                .corporation(testCorpPartyName)
                .build();

        exchangeWebClient.post()
                .uri(CUSTODIAN_CORP_URI)
                .header("Workflow-Id", "IT-createCorporation")
                .body(BodyInserters.fromValue(createCorporation))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(4)
    @DisplayName("CreateCorporation_AlreadyExists")
    void Given_CorporationAlreadyExists_When_CreatingCorporation_Then_ErrorReturned() {
        CreateCorporation createCorporation = CreateCorporation.builder()
                .corporation("Google")
                .build();

        exchangeWebClient.post()
                .uri(CUSTODIAN_CORP_URI)
                .header("Workflow-Id", "IT-createCorporation")
                .body(BodyInserters.fromValue(createCorporation))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(5)
    @DisplayName("CreateTrader_HappyPath")
    void Given_PartyExists_When_CreatingTrader_Then_TraderCreated() {
        String testTraderPartyName = "TestTrader-" + RandomStringUtils.randomAlphanumeric(8);
        TestUtils.createParty(exchangeWebClient, testTraderPartyName);

        CreateTrader createTrader = CreateTrader.builder()
                .trader(testTraderPartyName)
                .build();

        exchangeWebClient.post()
                .uri(CUSTODIAN_TRADER_URI)
                .header("Workflow-Id", "IT-createTrader")
                .body(BodyInserters.fromValue(createTrader))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(6)
    @DisplayName("CreateTrader_NoLedgerParty")
    void Given_NoLedgerPartyExists_When_CreatingTrader_Then_ErrorReturned() {
        String testTraderPartyName = "TestTrader-" + RandomStringUtils.randomAlphanumeric(8);
        CreateTrader createTrader = CreateTrader.builder()
                .trader(testTraderPartyName)
                .build();

        exchangeWebClient.post()
                .uri(CUSTODIAN_TRADER_URI)
                .header("Workflow-Id", "IT-createTrader")
                .body(BodyInserters.fromValue(createTrader))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(7)
    @DisplayName("CreateTrader_AlreadyExists")
    void Given_TraderAlreadyExists_When_CreatingTrader_Then_ErrorReturned() {
        CreateTrader createTrader = CreateTrader.builder()
                .trader("Alice")
                .build();

        exchangeWebClient.post()
                .uri(CUSTODIAN_TRADER_URI)
                .header("Workflow-Id", "IT-createTrader")
                .body(BodyInserters.fromValue(createTrader))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(8)
    @DisplayName("MatchOffers_HappyPath")
    void Given_MatchingBuyAndSell_When_MatchingOffers_Then_MatchProcessed() {
        Double amount = RandomUtils.nextInt(1000, 10000000) / 100.0;
        CreateCashRequest createCashRequest = CreateCashRequest.builder()
                .amount(amount)
                .currency(Currency.getInstance("EUR"))
                .build();

        CreateShareRequest createShareRequest = CreateShareRequest.builder().value(amount).build();

        TestUtils.createCash(exchangeWebClient, traderWebClient, "Alice", Role.TRDR, createCashRequest);
        TestUtils.createShares(exchangeWebClient, corporationWebClient, createShareRequest);

        TestUtils.createMatchingBuyAndSellOffers(traderWebClient, corporationWebClient, "EUR", amount, 1.0);

        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "EUR");
        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", amount, null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "currency", currencyFilter,
                "amount", amountFilter
        );
        FilterRequest filterRequest = FilterRequest.builder().filterModel(filterModel).build();

        ThrowingRunnable assertion = () -> {
            List<BuyOffer> buyOffers = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, "/exchange/buy/filters", BuyOffer.class);
            List<SellOffer> sellOffers = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, "/exchange/sell/filters", SellOffer.class);

            Assertions.assertFalse(buyOffers.isEmpty());
            Assertions.assertFalse(sellOffers.isEmpty());

            MatchOffers matchOffers = MatchOffers.builder()
                    .buyOfferIdentifier(buyOffers.get(0).getIdentifier())
                    .sellOfferIdentifier(sellOffers.get(0).getIdentifier())
                    .build();

            exchangeWebClient.post()
                    .uri(CUSTODIAN_MATCH_URI)
                    .header("Workflow-Id", "IT-matchOffers")
                    .body(BodyInserters.fromValue(matchOffers))
                    .exchange()
                    .expectStatus().is2xxSuccessful();
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(9)
    @DisplayName("MatchOffers_BadIdentifiers")
    void Given_BadIdentifiers_When_MatchingOffers_Then_ErrorReturned() {
        MatchOffers matchOffers = MatchOffers.builder()
                .buyOfferIdentifier(RandomStringUtils.randomAlphabetic(8))
                .sellOfferIdentifier(RandomStringUtils.randomAlphabetic(8))
                .build();

        exchangeWebClient.post()
                .uri(CUSTODIAN_MATCH_URI)
                .header("Workflow-Id", "IT-matchOffers")
                .body(BodyInserters.fromValue(matchOffers))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(10)
    @DisplayName("MatchOffers_MismatchedOffers")
    void Given_MismatchedOffers_When_MatchingOffers_Then_ErrorReturned() {
        Double amount = RandomUtils.nextInt(1000, 10000000) / 100.0;
        CreateCashRequest createCashRequest = CreateCashRequest.builder()
                .amount(amount)
                .currency(Currency.getInstance("EUR"))
                .build();

        CreateShareRequest createShareRequest = CreateShareRequest.builder().value(amount).build();

        TestUtils.createCash(exchangeWebClient, traderWebClient, "Alice", Role.TRDR, createCashRequest);
        TestUtils.createShares(exchangeWebClient, corporationWebClient, createShareRequest);

        Offer buyOffer = Offer.builder().corp("Google").amount(BigDecimal.valueOf(amount)).pricePerShare(1.0).currency("EUR").build();
        TestUtils.createBuyOffer(traderWebClient, Role.TRDR, buyOffer);

        Offer sellOffer = Offer.builder().corp("Google").amount(BigDecimal.valueOf(amount)).pricePerShare(3.5).currency("GBP").build();
        TestUtils.createSellOffer(corporationWebClient, Role.CORP, sellOffer);

        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "Google");
        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", amount, null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "corporation", currencyFilter,
                "amount", amountFilter
        );
        FilterRequest filterRequest = FilterRequest.builder().filterModel(filterModel).build();

        ThrowingRunnable assertion = () -> {
            List<BuyOffer> buyOffers = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, "/exchange/buy/filters", BuyOffer.class);
            List<SellOffer> sellOffers = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, "/exchange/sell/filters", SellOffer.class);

            Assertions.assertFalse(buyOffers.isEmpty());
            Assertions.assertFalse(sellOffers.isEmpty());

            MatchOffers matchOffers = MatchOffers.builder()
                    .buyOfferIdentifier(buyOffers.get(0).getIdentifier())
                    .sellOfferIdentifier(sellOffers.get(0).getIdentifier())
                    .build();

            exchangeWebClient.post()
                    .uri(CUSTODIAN_MATCH_URI)
                    .header("Workflow-Id", "IT-matchOffers")
                    .body(BodyInserters.fromValue(matchOffers))
                    .exchange()
                    .expectStatus().is4xxClientError();
        };

        TestUtils.awaitUntilAsserted(assertion);
    }
}