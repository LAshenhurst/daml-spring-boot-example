package com.lashe.example.api.controller;

import com.lashe.example.api.config.TestUtils;
import com.lashe.example.api.config.WebFluxTestConfig;
import com.lashe.example.api.domain.Role;
import com.lashe.example.api.domain.asset.cash.CreateCashRequest;
import com.lashe.example.api.domain.exchange.Offer;
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
class TraderControllerIT {
    @Autowired
    private WebTestClient traderWebClient;

    @Autowired
    private WebTestClient exchangeWebClient;

    @Autowired
    private WebTestClient corporationWebClient;

    private static final String TRADER_URI = "/trader";
    private static final String TRADER_CASH_REQUEST_URI = TRADER_URI + "/cash-request";
    private static final String TRADER_BUY_OFFER_URI = TRADER_URI + "/buy-offer";
    private static final String TRADER_SELL_OFFER_URI = TRADER_URI + "/sell-offer";

    @Test
    @Order(1)
    @DisplayName("CreateCashRequest_HappyPath")
    void Given_ValidCashRequest_When_CreatingCashRequest_Then_RequestCreated() {
        CreateCashRequest createCashRequest = CreateCashRequest.builder()
                .amount(300.0)
                .currency(Currency.getInstance("JPY"))
                .build();

        traderWebClient.post()
                .uri(TRADER_CASH_REQUEST_URI)
                .header("Workflow-Id", "IT-createCashRequest")
                .body(BodyInserters.fromValue(createCashRequest))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(2)
    @DisplayName("CreateCashRequest_PermissionDenied")
    void Given_WrongUser_When_CreatingCashRequest_Then_PermissionDenied() {
        CreateCashRequest createCashRequest = CreateCashRequest.builder()
                .amount(300.0)
                .currency(Currency.getInstance("NZD"))
                .build();

        corporationWebClient.post()
                .uri(TRADER_CASH_REQUEST_URI)
                .header("Workflow-Id", "IT-createCashRequest")
                .body(BodyInserters.fromValue(createCashRequest))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @Order(3)
    @DisplayName("CreateBuyOffer_HappyPath")
    void Given_ValidBuyOffer_When_CreatingBuyOffer_Then_BuyOfferCreated() {
        CreateCashRequest createCashRequest = CreateCashRequest.builder()
                .amount(300.0)
                .currency(Currency.getInstance("NZD"))
                .build();
        TestUtils.createCash(exchangeWebClient, traderWebClient, "Alice", Role.TRDR, createCashRequest);

        Offer offer = Offer.builder()
                .corp("Google")
                .amount(BigDecimal.valueOf(300.0))
                .pricePerShare(1.0)
                .currency("NZD")
                .build();

        traderWebClient.post()
                .uri(TRADER_BUY_OFFER_URI)
                .header("Workflow-Id", "IT-createBuyOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(4)
    @DisplayName("CreateBuyOffer_InsufficientCash")
    void Given_InsufficientCash_When_CreatingBuyOffer_Then_ErrorReturned() {
        TestUtils.cancelBuyOffers(traderWebClient, "Alice");
        Offer offer = Offer.builder()
                .corp("Google")
                .amount(BigDecimal.valueOf(300.0))
                .pricePerShare(300.0)
                .currency("NZD")
                .build();

        traderWebClient.post()
                .uri(TRADER_BUY_OFFER_URI)
                .header("Workflow-Id", "IT-createBuyOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(5)
    @DisplayName("CreateBuyOffer_PermissionDenied")
    void Given_WrongUser_When_CreatingBuyOffer_Then_PermissionDenied() {
        TestUtils.cancelBuyOffers(traderWebClient, "Alice");
        Offer offer = Offer.builder()
                .corp("Google")
                .amount(BigDecimal.valueOf(300.0))
                .pricePerShare(1.0)
                .currency("NZD")
                .build();

        corporationWebClient.post()
                .uri(TRADER_BUY_OFFER_URI)
                .header("Workflow-Id", "IT-createBuyOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(6)
    @DisplayName("CreateSellOffer_HappyPath")
    void Given_ValidSellOffer_When_CreatingSellOffer_Then_SellOfferCreated() {
        TestUtils.tradeShares(exchangeWebClient, traderWebClient, corporationWebClient, 300.0, 1.0, "NZD");

        Offer offer = Offer.builder()
                .corp("Google")
                .amount(BigDecimal.valueOf(300.0))
                .pricePerShare(1.0)
                .currency("USD")
                .build();

        traderWebClient.post()
                .uri(TRADER_SELL_OFFER_URI)
                .header("Workflow-Id", "IT-createSellOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    @Order(7)
    @DisplayName("CreateSellOffer_InsufficientShares")
    void Given_InsufficientShares_When_CreatingSellOffer_Then_ErrorReturned() {
        TestUtils.cancelSellOffers(traderWebClient, "Alice");
        Offer offer = Offer.builder()
                .corp("Google")
                .amount(BigDecimal.valueOf(99999999.0))
                .pricePerShare(1.0)
                .currency("USD")
                .build();

        traderWebClient.post()
                .uri(TRADER_SELL_OFFER_URI)
                .header("Workflow-Id", "IT-createSellOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(8)
    @DisplayName("CreateSellOffer_PermissionDenied")
    void Given_CorpUser_When_CreatingSellOffer_Then_PermissionDenied() {
        Offer offer = Offer.builder()
                .corp("Google")
                .amount(BigDecimal.valueOf(300.0))
                .pricePerShare(1.0)
                .currency("USD")
                .build();

        corporationWebClient.post()
                .uri(TRADER_SELL_OFFER_URI)
                .header("Workflow-Id", "IT-createSellOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().isForbidden();
    }
}