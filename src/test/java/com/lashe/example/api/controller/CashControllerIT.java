package com.lashe.example.api.controller;

import com.lashe.example.api.common.filters.ColumnFilter;
import com.lashe.example.api.common.filters.impl.NumberColumnFilter;
import com.lashe.example.api.common.filters.impl.TextColumnFilter;
import com.lashe.example.api.config.TestUtils;
import com.lashe.example.api.config.WebFluxTestConfig;
import com.lashe.example.api.domain.asset.AssetRequestResponse;
import com.lashe.example.api.domain.asset.AssetState;
import com.lashe.example.api.domain.asset.ChangeAssetState;
import com.lashe.example.api.domain.asset.cash.Cash;
import com.lashe.example.api.domain.asset.cash.CashRequest;
import com.lashe.example.api.domain.rest.request.FilterRequest;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.*;

@ExtendWith(SpringExtension.class)
@Import(WebFluxTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CashControllerIT {
    @Autowired
    private WebTestClient traderWebClient;

    @Autowired
    private WebTestClient exchangeWebClient;

    @Autowired
    private WebTestClient corporationWebClient;

    private static final String CASH_REQUEST_URI = "/cash/requests";
    private static final String CASH_REQUEST_FILTER_URI = CASH_REQUEST_URI + "/filters";
    private static final String CASH_REQUEST_ACCEPT = CASH_REQUEST_URI + "/accept";
    private static final String CASH_REQUEST_DECLINE = CASH_REQUEST_URI + "/decline";

    private static final String CASH_URI = "/cash";
    private static final String CASH_FILTER_URI = CASH_URI + "/filters";
    private static final String CASH_CHANGE_STATE_URI = CASH_URI + "/change-state";

    @Test
    @Order(1)
    @DisplayName("CashRequestFilter_HappyPath")
    void Given_CashRequestExists_When_FilteringCashRequests_Then_MatchingCashRequestReturned() {
        TestUtils.createCashRequest(corporationWebClient, "USD", 300.0);
        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "USD");
        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", 300.0, null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "currency", currencyFilter,
                "amount", amountFilter
        );
        FilterRequest filterRequest = FilterRequest.builder().filterModel(filterModel).build();

        ThrowingRunnable assertion = () -> {
            List<CashRequest> cashRequests = TestUtils.getFilterResponse(corporationWebClient, filterRequest, CASH_REQUEST_FILTER_URI, CashRequest.class);
            Assertions.assertFalse(cashRequests.isEmpty());
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(2)
    @DisplayName("CashRequestFilter_UnhappyPath")
    void Given_CashRequestExists_When_WrongUser_FiltersCashRequests_Then_CashRequestNOTReturned() {
        TestUtils.createCashRequest(corporationWebClient, "USD", 300.0);
        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "USD");
        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", 300.0, null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "currency", currencyFilter,
                "amount", amountFilter
        );
        FilterRequest filterRequest = FilterRequest.builder().filterModel(filterModel).build();

        ThrowingRunnable assertion = () -> {
            List<CashRequest> cashRequests = TestUtils.getFilterResponse(traderWebClient, filterRequest, CASH_REQUEST_FILTER_URI, CashRequest.class);
            Assertions.assertTrue(cashRequests.isEmpty());
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(3)
    @DisplayName("CashRequestAccept_HappyPath")
    void Given_CashRequestExists_When_AcceptingRequest_Then_CashCreated() {
        TestUtils.createCashRequest(corporationWebClient, "GBP", 450.0);

        AssetRequestResponse assetRequestResponse = AssetRequestResponse.builder()
                .currency(Currency.getInstance("GBP"))
                .requester("Google")
                .build();

        exchangeWebClient.post()
                .uri(CASH_REQUEST_ACCEPT)
                .header("Workflow-Id", "IT-acceptCashRequest")
                .body(BodyInserters.fromValue(assetRequestResponse))
                .exchange()
                .expectStatus().is2xxSuccessful();

        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "GBP");
        NumberColumnFilter amountFilter = new NumberColumnFilter("greaterThanOrEqual", 450.0, null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "currency", currencyFilter,
                "amount", amountFilter
        );
        FilterRequest filterRequest = FilterRequest.builder().filterModel(filterModel).build();
        ThrowingRunnable assertion = () -> {
            List<Cash> cash = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, CASH_FILTER_URI, Cash.class);
            Assertions.assertFalse(cash.isEmpty());
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(4)
    @DisplayName("CashRequestDecline_HappyPath")
    void Given_CashRequestExists_When_DecliningRequest_Then_CashNotCreated() {
        TestUtils.createCashRequest(corporationWebClient, "USD", 201.0);
        AssetRequestResponse assetRequestResponse = AssetRequestResponse.builder()
                .currency(Currency.getInstance("USD"))
                .requester("Google")
                .build();

        exchangeWebClient.post()
                .uri(CASH_REQUEST_DECLINE)
                .header("Workflow-Id", "IT-declineCashRequest")
                .body(BodyInserters.fromValue(assetRequestResponse))
                .exchange()
                .expectStatus().is2xxSuccessful();

        TextColumnFilter currencyFilter = new TextColumnFilter("equals", "USD");
        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", 201.0, null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "currency", currencyFilter,
                "amount", amountFilter
        );
        FilterRequest filterRequest = FilterRequest.builder().filterModel(filterModel).build();
        ThrowingRunnable assertion = () -> {
            List<Cash> cash = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, CASH_FILTER_URI, Cash.class);
            Assertions.assertTrue(cash.isEmpty());
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(5)
    @DisplayName("CashById_HappyPath")
    void Given_CashExists_When_GettingCashById_Then_MatchingCashReturned() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        List<Cash> cash = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, CASH_FILTER_URI, Cash.class);
        Assertions.assertFalse(cash.isEmpty());

        Cash returnedCash = exchangeWebClient.get()
                .uri(CASH_URI + "/" + cash.get(0).getIdentifier())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Cash.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(returnedCash);
        Assertions.assertEquals(cash.get(0), returnedCash);
    }

    @Test
    @Order(6)
    @DisplayName("CashById_UnhappyPath")
    void Given_CashExists_When_WrongUser_GetsCashById_Then_404Error() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        List<Cash> cashList = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, CASH_FILTER_URI, Cash.class);
        Assertions.assertFalse(cashList.isEmpty());

        Optional<Cash> corpCash = cashList.stream().filter(cash -> cash.getOwner().equals("Google")).findFirst();
        Assertions.assertTrue(corpCash.isPresent());

        traderWebClient.get()
                .uri(CASH_URI + "/" + corpCash.get().getIdentifier())
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(7)
    @DisplayName("ChangeCashState_HappyPath")
    void Given_CashExists_When_ChangingCashState_Then_CashStateChanged() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        List<Cash> cash = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, CASH_FILTER_URI, Cash.class);
        Assertions.assertFalse(cash.isEmpty());

        Assertions.assertNotEquals(AssetState.LOCKED, cash.get(0).getState());
        ChangeAssetState changeAssetState = ChangeAssetState.builder()
                .identifier(cash.get(0).getIdentifier())
                .amount(cash.get(0).getAmount())
                .newState(AssetState.LOCKED)
                .build();

        exchangeWebClient.post()
                .uri(CASH_CHANGE_STATE_URI)
                .body(BodyInserters.fromValue(changeAssetState))
                .header("Workflow-Id", "IT-changeCashState")
                .exchange()
                .expectStatus().is2xxSuccessful();

        TextColumnFilter ownerFilter = new TextColumnFilter("equals", cash.get(0).getOwner());
        TextColumnFilter currencyFilter = new TextColumnFilter("equals", cash.get(0).getCurrency().getCurrencyCode());
        TextColumnFilter stateFilter = new TextColumnFilter("equals", AssetState.LOCKED.name());
        Map<String, ColumnFilter> filterModel = Map.of(
                "owner", ownerFilter,
                "currency", currencyFilter,
                "state", stateFilter
        );
        FilterRequest stateFilterRequest = FilterRequest.builder().filterModel(filterModel).build();
        ThrowingRunnable assertion = () -> {
            List<Cash> updatedCash = TestUtils.getFilterResponse(exchangeWebClient, stateFilterRequest, CASH_FILTER_URI, Cash.class);
            Assertions.assertFalse(updatedCash.isEmpty());
        };

        TestUtils.awaitUntilAsserted(assertion);
    }
}