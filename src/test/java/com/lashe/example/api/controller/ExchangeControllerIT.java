package com.lashe.example.api.controller;

import com.lashe.example.api.common.filters.ColumnFilter;
import com.lashe.example.api.common.filters.impl.NumberColumnFilter;
import com.lashe.example.api.common.filters.impl.TextColumnFilter;
import com.lashe.example.api.config.TestUtils;
import com.lashe.example.api.config.WebFluxTestConfig;
import com.lashe.example.api.domain.Role;
import com.lashe.example.api.domain.asset.cash.CreateCashRequest;
import com.lashe.example.api.domain.asset.share.CreateShareRequest;
import com.lashe.example.api.domain.exchange.BuyOffer;
import com.lashe.example.api.domain.exchange.Offer;
import com.lashe.example.api.domain.exchange.SellOffer;
import com.lashe.example.api.domain.rest.request.FilterRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.*;

@ExtendWith(SpringExtension.class)
@Import(WebFluxTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExchangeControllerIT {
    @Autowired
    private WebTestClient traderWebClient;

    @Autowired
    private WebTestClient exchangeWebClient;

    @Autowired
    private WebTestClient corporationWebClient;

    private static final String EXCHANGE_URI = "/exchange";
    private static final String EXCHANGE_BUY_OFFER_URI = EXCHANGE_URI + "/buy";
    private static final String EXCHANGE_SELL_OFFER_URI = EXCHANGE_URI + "/sell";

    @Test
    @Order(1)
    @DisplayName("BuyOfferFilter_HappyPath")
    void Given_ExistingCash_When_CreatingBuyOffer_Then_BuyOfferCreated() {
        CreateCashRequest createCashRequest = CreateCashRequest.builder().currency(Currency.getInstance("NZD")).amount(300.0).build();
        TestUtils.createCash(exchangeWebClient, traderWebClient, "Alice", Role.TRDR, createCashRequest);

        Offer offer = Offer.builder().currency("NZD").pricePerShare(1.0).amount(BigDecimal.valueOf(300.0)).corp("Google").build();
        TestUtils.createBuyOffer(traderWebClient, Role.TRDR, offer);

        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "NZD");
        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", 300.0, null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "currency", currencyFilter,
                "amount", amountFilter
        );
        FilterRequest filterRequest = FilterRequest.builder().filterModel(filterModel).build();

        ThrowingRunnable assertion = () -> {
            List<BuyOffer> buyOffers = TestUtils.getFilterResponse(traderWebClient, filterRequest, EXCHANGE_BUY_OFFER_URI + "/filters", BuyOffer.class);
            Assertions.assertFalse(buyOffers.isEmpty());
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(2)
    @DisplayName("BuyOfferById_HappyPath")
    void Given_BuyOfferExists_When_GettingBuyOfferById_Then_BuyOfferReturned() {
        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "NZD");
        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", 300.0, null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "currency", currencyFilter,
                "amount", amountFilter
        );
        FilterRequest filterRequest = FilterRequest.builder().filterModel(filterModel).build();

        List<BuyOffer> buyOffers = TestUtils.getFilterResponse(traderWebClient, filterRequest, EXCHANGE_BUY_OFFER_URI + "/filters", BuyOffer.class);
        Assertions.assertFalse(buyOffers.isEmpty());

        BuyOffer buyOffer = traderWebClient.get()
                .uri(EXCHANGE_BUY_OFFER_URI + "/" + buyOffers.get(0).getIdentifier())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(BuyOffer.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(buyOffer);
        Assertions.assertEquals("NZD", buyOffer.getCurrency().getCurrencyCode());
        Assertions.assertEquals(300.0, buyOffer.getAmount().doubleValue());
    }

    @Test
    @Order(3)
    @DisplayName("BuyOfferById_BadId")
    void Given_BadIdentifier_When_GettingBuyOfferById_Then_404Returned() {
        traderWebClient.get()
                .uri(EXCHANGE_BUY_OFFER_URI + "/" + RandomStringUtils.randomAlphanumeric(12))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(4)
    @DisplayName("BuyOfferById_WrongUser")
    void Given_WrongUser_When_Getting_BuyOfferById_Then_404Returned() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        List<BuyOffer> buyOffers = TestUtils.getFilterResponse(traderWebClient, filterRequest, EXCHANGE_BUY_OFFER_URI + "/filters", BuyOffer.class);
        Assertions.assertFalse(buyOffers.isEmpty());

        Optional<BuyOffer> aliceOffer = buyOffers.stream()
                .filter(offer -> offer.getBuyer().equals("Alice"))
                .findFirst();
        Assertions.assertTrue(aliceOffer.isPresent());

        corporationWebClient.get()
                .uri(EXCHANGE_BUY_OFFER_URI + "/" + aliceOffer.get().getIdentifier())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(5)
    @DisplayName("BuyOfferFilters_DifferentUsers")
    void Given_TwoUsers_When_FilteringBuyOffers_Then_FilterResponseNotEqual() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        List<BuyOffer> exchangeBuyOffers = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, EXCHANGE_BUY_OFFER_URI + "/filters", BuyOffer.class);
        List<BuyOffer> corpBuyOffers = TestUtils.getFilterResponse(corporationWebClient, filterRequest, EXCHANGE_BUY_OFFER_URI + "/filters", BuyOffer.class);

        Assertions.assertNotEquals(exchangeBuyOffers, corpBuyOffers);
    }

    @Test
    @Order(6)
    @DisplayName("BuyOfferFilter_BadFilter")
    void Given_InvalidCurrency_When_FilteringBuyOffers_Then_EmptyListReturned() {
        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "AAA");
        FilterRequest filterRequest = FilterRequest.builder().filterModel(Collections.singletonMap("currency", currencyFilter)).build();

        List<BuyOffer> buyOffers = TestUtils.getFilterResponse(traderWebClient, filterRequest, EXCHANGE_BUY_OFFER_URI + "/filters", BuyOffer.class);
        Assertions.assertTrue(buyOffers.isEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("SellOfferFilter_HappyPath")
    void Given_ExistingShares_When_CreatingSellOffer_Then_SellOfferCreated() {
        CreateShareRequest createShareRequest = CreateShareRequest.builder().value(300.0).build();
        TestUtils.createShares(exchangeWebClient, corporationWebClient, createShareRequest);

        Offer offer = Offer.builder().currency("JPY").pricePerShare(1.0).amount(BigDecimal.valueOf(300.0)).corp("Google").build();
        TestUtils.createSellOffer(corporationWebClient, Role.CORP, offer);

        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "JPY");
        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", 300.0, null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "currency", currencyFilter,
                "amount", amountFilter
        );
        FilterRequest filterRequest = FilterRequest.builder().filterModel(filterModel).build();

        ThrowingRunnable assertions = () -> {
            List<SellOffer> sellOffers = TestUtils.getFilterResponse(corporationWebClient, filterRequest, EXCHANGE_SELL_OFFER_URI + "/filters", SellOffer.class);
            Assertions.assertFalse(sellOffers.isEmpty());
        };

        TestUtils.awaitUntilAsserted(assertions);
    }

    @Test
    @Order(8)
    @DisplayName("SellOfferById_HappyPath")
    void Given_SellOfferExists_When_GettingSellOfferById_Then_SellOfferReturned() {
        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "JPY");
        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", 300.0, null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "currency", currencyFilter,
                "amount", amountFilter
        );
        FilterRequest filterRequest = FilterRequest.builder().filterModel(filterModel).build();

        List<SellOffer> sellOffers = TestUtils.getFilterResponse(corporationWebClient, filterRequest, EXCHANGE_SELL_OFFER_URI + "/filters", SellOffer.class);
        Assertions.assertFalse(sellOffers.isEmpty());

        SellOffer sellOffer = corporationWebClient.get()
                .uri(EXCHANGE_SELL_OFFER_URI + "/" + sellOffers.get(0).getIdentifier())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(SellOffer.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(sellOffer);
        Assertions.assertEquals("JPY", sellOffer.getCurrency().getCurrencyCode());
        Assertions.assertEquals(300.0, sellOffer.getAmount().doubleValue());
    }

    @Test
    @Order(9)
    @DisplayName("SellOfferById_BadId")
    void Given_BadIdentifier_When_GettingSellOfferById_Then_404Returned() {
        traderWebClient.get()
                .uri(EXCHANGE_SELL_OFFER_URI + "/" + RandomStringUtils.randomAlphanumeric(12))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(10)
    @DisplayName("SellOfferById_WrongUser")
    void Given_WrongUser_When_Getting_SellOfferById_Then_404Returned() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        List<SellOffer> sellOffers = TestUtils.getFilterResponse(corporationWebClient, filterRequest, EXCHANGE_SELL_OFFER_URI + "/filters", SellOffer.class);
        Assertions.assertFalse(sellOffers.isEmpty());

        Optional<SellOffer> googleOffer = sellOffers.stream()
                .filter(offer -> offer.getSeller().equals("Google"))
                .findFirst();
        Assertions.assertTrue(googleOffer.isPresent());

        traderWebClient.get()
                .uri(EXCHANGE_SELL_OFFER_URI + "/" + googleOffer.get().getIdentifier())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(11)
    @DisplayName("SellOfferFilters_DifferentUsers")
    void Given_TwoUsers_When_FilteringSellOffers_Then_FilterResponseNotEqual() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        List<SellOffer> exchangeSellOffers = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, EXCHANGE_SELL_OFFER_URI + "/filters", SellOffer.class);
        List<SellOffer> traderSellOffers = TestUtils.getFilterResponse(traderWebClient, filterRequest, EXCHANGE_SELL_OFFER_URI + "/filters", SellOffer.class);

        Assertions.assertNotEquals(exchangeSellOffers, traderSellOffers);
    }

    @Test
    @Order(12)
    @DisplayName("SellOfferFilter_BadFilter")
    void Given_InvalidCurrency_When_FilteringSellOffers_Then_EmptyListReturned() {
        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "AAA");
        FilterRequest filterRequest = FilterRequest.builder().filterModel(Collections.singletonMap("currency", currencyFilter)).build();

        List<SellOffer> sellOffers = TestUtils.getFilterResponse(traderWebClient, filterRequest, EXCHANGE_SELL_OFFER_URI + "/filters", SellOffer.class);
        Assertions.assertTrue(sellOffers.isEmpty());
    }

    @Test
    @Order(13)
    @DisplayName("CancelBuyOffer_HappyPath")
    void Given_BuyOfferExists_When_CancellingBuyOffer_Then_BuyOfferCancelled() {
        TestUtils.cancelBuyOffers(traderWebClient, "Alice");
        CreateCashRequest createCashRequest = CreateCashRequest.builder().amount(500.0).currency(Currency.getInstance("USD")).build();
        TestUtils.createCash(exchangeWebClient, traderWebClient, "Alice", Role.TRDR, createCashRequest);

        Offer offer = Offer.builder()
                .corp("Google")
                .amount(BigDecimal.valueOf(500.0))
                .pricePerShare(1.0)
                .currency("USD")
                .build();
        TestUtils.createBuyOffer(traderWebClient, Role.TRDR, offer);
        ThrowingRunnable assertion = () -> {
            FilterRequest filterRequest = FilterRequest.builder().build();
            List<BuyOffer> buyOffers = TestUtils.getFilterResponse(traderWebClient, filterRequest, EXCHANGE_BUY_OFFER_URI + "/filters", BuyOffer.class);
            Assertions.assertFalse(buyOffers.isEmpty());

            traderWebClient.delete()
                    .uri(EXCHANGE_BUY_OFFER_URI + "/" + buyOffers.get(0).getIdentifier())
                    .header("Workflow-Id", "IT-cancelBuyOffer")
                    .exchange()
                    .expectStatus().is2xxSuccessful();
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(14)
    @DisplayName("CancelBuyOffer_BadId")
    void Given_BadIdentifier_When_CancellingBuyOffer_Then_ErrorReturned() {
        traderWebClient.delete()
                .uri(EXCHANGE_BUY_OFFER_URI + "/" + RandomStringUtils.randomAlphanumeric(12))
                .header("Workflow-Id", "IT-cancelBuyOffer")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(15)
    @DisplayName("CancelSellOffer_HappyPath")
    void Given_SellOfferExists_When_CancellingSellOffer_Then_SellOfferCancelled() {
        TestUtils.cancelSellOffers(corporationWebClient, "Google");
        TestUtils.createShares(exchangeWebClient, corporationWebClient, CreateShareRequest.builder().value(500.0).build());

        Offer offer = Offer.builder()
                .corp("Google")
                .amount(BigDecimal.valueOf(500.0))
                .pricePerShare(1.0)
                .currency("USD")
                .build();
        TestUtils.createSellOffer(corporationWebClient, Role.CORP, offer);
        ThrowingRunnable assertion = () -> {
            FilterRequest filterRequest = FilterRequest.builder().build();
            List<SellOffer> sellOffers   = TestUtils.getFilterResponse(corporationWebClient, filterRequest, EXCHANGE_SELL_OFFER_URI + "/filters", SellOffer.class);
            Assertions.assertFalse(sellOffers.isEmpty());

            corporationWebClient.delete()
                    .uri(EXCHANGE_SELL_OFFER_URI + "/" + sellOffers.get(0).getIdentifier())
                    .header("Workflow-Id", "IT-cancelSellOffer")
                    .exchange()
                    .expectStatus().is2xxSuccessful();
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(16)
    @DisplayName("CancelSellOffer_BadId")
    void Given_BadIdentifier_When_CancellingSellOffer_Then_ErrorReturned() {
        corporationWebClient.delete()
                .uri(EXCHANGE_SELL_OFFER_URI + "/" + RandomStringUtils.randomAlphanumeric(12))
                .header("Workflow-Id", "IT-cancelSellOffer")
                .exchange()
                .expectStatus().is4xxClientError();
    }
}