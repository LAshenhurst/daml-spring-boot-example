package com.example.api.controller;

import com.example.api.config.TestUtils;
import com.example.api.config.WebFluxTestConfig;
import com.example.api.domain.asset.cash.CreateCashRequest;
import com.example.api.domain.asset.share.CreateShareRequest;
import com.example.api.domain.exchange.Offer;
import org.apache.commons.lang3.RandomStringUtils;
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

@ExtendWith(SpringExtension.class)
@Import(WebFluxTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CorporationControllerIT {
    @Autowired
    private WebTestClient traderWebClient;

    @Autowired
    private WebTestClient exchangeWebClient;

    @Autowired
    private WebTestClient corporationWebClient;

    private static final String CORPORATION_URI = "/corporation";
    private static final String CORP_SHARE_REQUEST_URI = CORPORATION_URI + "/share-request";
    private static final String CORP_CASH_REQUEST_URI = CORPORATION_URI + "/cash-request";
    private static final String CORP_BUY_OFFER_URI = CORPORATION_URI + "/buy-offer";
    private static final String CORP_SELL_OFFER_URI = CORPORATION_URI + "/sell-offer";

    @Test
    @Order(1)
    @DisplayName("CreateShareRequest_HappyPath")
    void Given_ValidShareRequest_When_CreatingShareRequest_Then_ShareRequestCreated() {
        CreateShareRequest createShareRequest = CreateShareRequest.builder().value(300.0).build();

        corporationWebClient.post()
                .uri(CORP_SHARE_REQUEST_URI)
                .header("Workflow-Id", "IT-createShareRequest")
                .body(BodyInserters.fromValue(createShareRequest))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(2)
    @DisplayName("CreateShareRequest_WrongUser")
    void Given_Trader_When_CreatingShareRequest_Then_PermissionDenied() {
        CreateShareRequest createShareRequest = CreateShareRequest.builder().value(300.0).build();

        traderWebClient.post()
                .uri(CORP_SHARE_REQUEST_URI)
                .header("Workflow-Id", "IT-createShareRequest")
                .body(BodyInserters.fromValue(createShareRequest))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(3)
    @DisplayName("CreateCashRequest_HappyPath")
    void Given_ValidCash_Request_When_CreatingCashRequest_Then_CashRequestCreated() {
        CreateCashRequest createCashRequest = CreateCashRequest.builder().currency(Currency.getInstance("EUR")).amount(300.0).build();

        corporationWebClient.post()
                .uri(CORP_CASH_REQUEST_URI)
                .header("Workflow-Id", "IT-createCashRequest")
                .body(BodyInserters.fromValue(createCashRequest))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(4)
    @DisplayName("CreateCashRequest_WrongUser")
    void Given_Trader_When_CreatingCorpCashRequest_Then_PermissionDenied() {
        CreateCashRequest createCashRequest = CreateCashRequest.builder().currency(Currency.getInstance("EUR")).amount(300.0).build();

        traderWebClient.post()
                .uri(CORP_CASH_REQUEST_URI)
                .header("Workflow-Id", "IT-createCashRequest")
                .body(BodyInserters.fromValue(createCashRequest))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(5)
    @DisplayName("CreateBuyOffer_HappyPath")
    void Given_ValidBuyOffer_And_ExistingCash_When_CreatingBuyOffer_Then_BuyOfferCreated() {
        TestUtils.acceptCashRequest(exchangeWebClient, "EUR");

        Offer offer = Offer.builder()
                .amount(BigDecimal.valueOf(100.0))
                .corp(RandomStringUtils.randomAlphabetic(4))
                .currency("EUR")
                .pricePerShare(3.0)
                .build();

        corporationWebClient.post()
                .uri(CORP_BUY_OFFER_URI)
                .header("Workflow-Id", "IT-corpCreateBuyOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(6)
    @DisplayName("CreateBuyOffer_InsufficientCash")
    void Given_InsufficientCash_When_CreatingBuyOffer_Then_400Returned() {
        TestUtils.acceptCashRequest(exchangeWebClient, "EUR");

        Offer offer = Offer.builder()
                .amount(BigDecimal.valueOf(10000.0))
                .corp("TSLA")
                .currency("EUR")
                .pricePerShare(300.0)
                .build();

        corporationWebClient.post()
                .uri(CORP_BUY_OFFER_URI)
                .header("Workflow-Id", "IT-corpCreateBuyOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(7)
    @DisplayName("CreateBuyOffer_WrongUser")
    void Given_WrongUser_When_CreatingCorpBuyOffer_Then_PermissionDenied() {
        TestUtils.acceptCashRequest(exchangeWebClient, "EUR");

        Offer offer = Offer.builder()
                .amount(BigDecimal.valueOf(100.0))
                .corp("TSLA")
                .currency("EUR")
                .pricePerShare(3.0)
                .build();

        traderWebClient.post()
                .uri(CORP_BUY_OFFER_URI)
                .header("Workflow-Id", "IT-corpCreateBuyOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(8)
    @DisplayName("CreateSellOffer_HappyPath")
    void Given_SharesExisting_When_CreateSellOffer_Then_SellOfferCreated() {
        TestUtils.acceptShareRequest(exchangeWebClient);

        Offer offer = Offer.builder()
                .amount(BigDecimal.valueOf(300.0))
                .corp("Google")
                .pricePerShare(2.5)
                .currency("EUR")
                .build();

        corporationWebClient.post()
                .uri(CORP_SELL_OFFER_URI)
                .header("Workflow-Id", "IT-corpCreateSellOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(9)
    @DisplayName("CreateSellOffer_WrongUser")
    void Given_Trader_When_CreateCorpSellOffer_Then_PermissionDenied() {
        TestUtils.acceptShareRequest(exchangeWebClient);

        Offer offer = Offer.builder()
                .amount(BigDecimal.valueOf(300.0))
                .corp("Google")
                .pricePerShare(2.5)
                .currency("EUR")
                .build();

        traderWebClient.post()
                .uri(CORP_SELL_OFFER_URI)
                .header("Workflow-Id", "IT-corpCreateSellOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(10)
    @DisplayName("CreateSellOffer_InsufficientShares")
    void Given_InsufficientShares_When_CreatingSellOffer_Then_400Returned() {
        TestUtils.acceptShareRequest(exchangeWebClient);

        Offer offer = Offer.builder()
                .amount(BigDecimal.valueOf(30000000.0))
                .corp("Google")
                .pricePerShare(2.5)
                .currency("EUR")
                .build();

        corporationWebClient.post()
                .uri(CORP_SELL_OFFER_URI)
                .header("Workflow-Id", "IT-corpCreateSellOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is4xxClientError();
    }
}