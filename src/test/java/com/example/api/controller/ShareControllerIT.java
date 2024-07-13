package com.example.api.controller;

import com.example.api.common.filters.ColumnFilter;
import com.example.api.common.filters.impl.NumberColumnFilter;
import com.example.api.common.filters.impl.TextColumnFilter;
import com.example.api.config.TestUtils;
import com.example.api.config.WebFluxTestConfig;
import com.example.api.domain.asset.AssetRequestResponse;
import com.example.api.domain.asset.share.CreateShareRequest;
import com.example.api.domain.asset.share.Share;
import com.example.api.domain.asset.share.ShareRequest;
import com.example.api.domain.rest.request.FilterRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@Import(WebFluxTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ShareControllerIT {
    @Autowired
    private WebTestClient traderWebClient;

    @Autowired
    private WebTestClient exchangeWebClient;

    @Autowired
    private WebTestClient corporationWebClient;

    private static final String SHARES_URI = "/shares";
    private static final String SHARES_FILTER_URI = SHARES_URI + "/filters";
    private static final String SHARE_REQUESTS_URI = SHARES_URI + "/requests";
    private static final String SHARE_REQUESTS_FILTER_URI = SHARE_REQUESTS_URI + "/filters";

    @Test
    @Order(1)
    @DisplayName("SharesFilter_HappyPath")
    void Given_SharesExist_When_FilteringShares_Then_MatchingSharesReturned() {
        CreateShareRequest createShareRequest = CreateShareRequest.builder().value(150.0).build();
        TestUtils.createShares(exchangeWebClient, corporationWebClient, createShareRequest);

        TextColumnFilter corpFilter = new TextColumnFilter("equals", "Google");
        NumberColumnFilter amountFilter = new NumberColumnFilter("greaterThanOrEqual", 150.0,  null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "owner", corpFilter,
                "corporation", corpFilter,
                "amount", amountFilter
        );
        FilterRequest filterRequest = FilterRequest.builder().filterModel(filterModel).build();

        ThrowingRunnable assertion = () -> {
            List<Share> shares = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, SHARES_FILTER_URI, Share.class);
            Assertions.assertFalse(shares.isEmpty());
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(2)
    @DisplayName("SharesFilter_UserPermission")
    void Given_TraderUser_When_FilteringShares_Then_CorporationSharesNotVisible() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        List<Share> shares = TestUtils.getFilterResponse(traderWebClient, filterRequest, SHARES_FILTER_URI, Share.class);
        Assertions.assertTrue(shares.stream().allMatch(share -> share.getOwner().equals("Alice")));
    }

    @Test
    @Order(3)
    @DisplayName("GetSharesById_HappyPath")
    void Given_SharesExist_When_GettingSharesById_Then_SharesReturned() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        List<Share> shares = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, SHARES_FILTER_URI, Share.class);
        Assertions.assertFalse(shares.isEmpty());

        Share share = exchangeWebClient.get()
                .uri(SHARES_URI + "/" + shares.get(0).getIdentifier())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Share.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(share);
        Assertions.assertEquals(shares.get(0), share);
    }

    @Test
    @Order(4)
    @DisplayName("GetSharesById_WrongUser")
    void Given_TraderUser_When_GettingCorporationSharesById_Then_404Returned() {
        TextColumnFilter corpFilter = new TextColumnFilter("equals", "Google");
        FilterRequest filterRequest = FilterRequest.builder().filterModel(Collections.singletonMap("owner", corpFilter)).build();
        List<Share> shares = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, SHARES_FILTER_URI, Share.class);
        Assertions.assertFalse(shares.isEmpty());

        traderWebClient.get()
                .uri(SHARES_URI + "/" + shares.get(0).getIdentifier())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(5)
    @DisplayName("GetSharesById_BadId")
    void Given_BadIdentifier_When_GettingSharesById_Then_404Returned() {
        traderWebClient.get()
                .uri(SHARES_URI + "/" + RandomStringUtils.randomAlphanumeric(12))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(6)
    @DisplayName("ShareRequestsFilter_HappyPath")
    void Given_ShareRequestExists_When_FilteringShareRequests_Then_ShareRequestsReturned() {
        TestUtils.createShareRequest(corporationWebClient, 450.0);

        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", 450.0, null);
        FilterRequest filterRequest = FilterRequest.builder().filterModel(Collections.singletonMap("amount", amountFilter)).build();

        ThrowingRunnable assertion = () -> {
            List<ShareRequest> shareRequests = TestUtils.getFilterResponse(exchangeWebClient, filterRequest, SHARE_REQUESTS_FILTER_URI, ShareRequest.class);
            Assertions.assertFalse(shareRequests.isEmpty());
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(7)
    @DisplayName("ShareRequestsFilter_UserPermission")
    void Given_CorporateUser_When_FilteringShareRequests_Then_OnlyCorporationShareRequestsVisible() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        ThrowingRunnable assertion = () -> {
            List<ShareRequest> shareRequests = TestUtils.getFilterResponse(corporationWebClient, filterRequest, SHARE_REQUESTS_FILTER_URI, ShareRequest.class);
            Assertions.assertTrue(shareRequests.stream().allMatch(shareRequest -> shareRequest.getRequester().equals("Google")));
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(8)
    @DisplayName("AcceptShareRequest_HappyPath")
    void Given_ValidShareRequest_When_AcceptingShareRequest_Then_NewSharesCreated() {
        TextColumnFilter requesterFilter = new TextColumnFilter("equals", "Google");
        FilterRequest filterRequest = FilterRequest.builder().filterModel(Collections.singletonMap("requester", requesterFilter)).build();
        List<ShareRequest> shareRequests = TestUtils.getFilterResponse(corporationWebClient, filterRequest, SHARE_REQUESTS_FILTER_URI, ShareRequest.class);
        Assertions.assertEquals(1, shareRequests.size());

        AssetRequestResponse assetRequestResponse = AssetRequestResponse.builder().requester("Google").build();

        exchangeWebClient.post()
                .uri(SHARE_REQUESTS_URI + "/accept")
                .body(BodyInserters.fromValue(assetRequestResponse))
                .header("Workflow-Id", "IT-acceptShareRequest")
                .exchange()
                .expectStatus().is2xxSuccessful();

        NumberColumnFilter amountFilter = new NumberColumnFilter("greaterThanOrEqual", shareRequests.get(0).getAmount(), null);
        Map<String, ColumnFilter> sharesFilterModel = Map.of(
                "owner", requesterFilter,
                "corporation", requesterFilter,
                "amount", amountFilter
        );
        FilterRequest sharesFilterRequest = FilterRequest.builder().filterModel(sharesFilterModel).build();

        ThrowingRunnable assertion = () -> {
            List<Share> shares = TestUtils.getFilterResponse(exchangeWebClient, sharesFilterRequest, SHARES_FILTER_URI, Share.class);
            Assertions.assertEquals(1, shares.size());
            Assertions.assertTrue(shares.get(0).getAmount().doubleValue() >= shareRequests.get(0).getAmount());
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(9)
    @DisplayName("AcceptShareRequest_NoRequest")
    void Given_NoShareRequest_When_AcceptingShareRequest_Then_ErrorReturned() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        List<ShareRequest> shareRequests = TestUtils.getFilterResponse(corporationWebClient, filterRequest, SHARE_REQUESTS_FILTER_URI, ShareRequest.class);
        Assertions.assertTrue(shareRequests.isEmpty());

        AssetRequestResponse assetRequestResponse = AssetRequestResponse.builder().requester("Google").build();

        exchangeWebClient.post()
                .uri(SHARE_REQUESTS_URI + "/accept")
                .header("Workflow-Id", "IT-acceptShareRequest")
                .body(BodyInserters.fromValue(assetRequestResponse))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(10)
    @DisplayName("AcceptShareRequest_PermissionDenied")
    void Given_TraderUser_When_AcceptingShareRequest_Then_PermissionDenied() {
        AssetRequestResponse assetRequestResponse = AssetRequestResponse.builder().requester("Google").build();

        traderWebClient.post()
                .uri(SHARE_REQUESTS_URI + "/accept")
                .header("Workflow-Id", "IT-acceptShareRequest")
                .body(BodyInserters.fromValue(assetRequestResponse))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @Order(11)
    @DisplayName("DeclineShareRequest_HappyPath")
    void Given_ShareRequest_When_DecliningShareRequest_Then_SharesNotIncreased() {
        TextColumnFilter requesterFilter = new TextColumnFilter("equals", "Google");
        Map<String, ColumnFilter> shareFilterModel = Map.of(
                "owner", requesterFilter,
                "corporation", requesterFilter
        );

        FilterRequest shareFilterRequest = FilterRequest.builder().filterModel(shareFilterModel).build();
        FilterRequest shareRequestsFilterRequest = FilterRequest.builder().filterModel(Collections.singletonMap("requester", requesterFilter)).build();
        List<Share> shares = TestUtils.getFilterResponse(exchangeWebClient, shareFilterRequest, SHARES_FILTER_URI, Share.class);
        List<ShareRequest> shareRequests = TestUtils.getFilterResponse(corporationWebClient, shareRequestsFilterRequest, SHARE_REQUESTS_FILTER_URI, ShareRequest.class);
        Assertions.assertTrue(shareRequests.isEmpty());

        TestUtils.createShareRequest(corporationWebClient, 300.0);

        AssetRequestResponse assetRequestResponse = AssetRequestResponse.builder().requester("Google").build();

        exchangeWebClient.post()
                .uri(SHARE_REQUESTS_URI + "/decline")
                .header("Workflow-Id", "IT-declineShareRequest")
                .body(BodyInserters.fromValue(assetRequestResponse))
                .exchange()
                .expectStatus().is2xxSuccessful();


        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", 300.0, null);
        Map<String, ColumnFilter> updatedRequestFilterModel = Map.of(
                "requester", requesterFilter,
                "amount", amountFilter
        );
        FilterRequest updatedFilterRequest = FilterRequest.builder().filterModel(updatedRequestFilterModel).build();

        ThrowingRunnable assertion = () -> {
            List<ShareRequest> updatedShareRequests = TestUtils.getFilterResponse(corporationWebClient, updatedFilterRequest, SHARE_REQUESTS_FILTER_URI, ShareRequest.class);
            Assertions.assertEquals(0, updatedShareRequests.size());

            List<Share> updatedShares = TestUtils.getFilterResponse(exchangeWebClient, shareFilterRequest, SHARES_FILTER_URI, Share.class);
            Assertions.assertEquals(shares, updatedShares);
        };

        TestUtils.awaitUntilAsserted(assertion);
    }

    @Test
    @Order(12)
    @DisplayName("DeclineShareRequest_NoRequest")
    void Given_NoShareRequest_When_DecliningRequest_Then_ErrorReturned() {
        FilterRequest filterRequest = FilterRequest.builder().build();
        List<ShareRequest> shareRequests = TestUtils.getFilterResponse(corporationWebClient, filterRequest, SHARE_REQUESTS_FILTER_URI, ShareRequest.class);
        Assertions.assertTrue(shareRequests.isEmpty());

        AssetRequestResponse assetRequestResponse = AssetRequestResponse.builder().requester("Google").build();

        exchangeWebClient.post()
                .uri(SHARE_REQUESTS_URI + "/decline")
                .header("Workflow-Id", "IT-declineShareRequest")
                .body(BodyInserters.fromValue(assetRequestResponse))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(13)
    @DisplayName("DeclineShareRequest_PermissionDenied")
    void Given_TraderUser_When_DecliningShareRequest_Then_PermissionDenied() {
        AssetRequestResponse assetRequestResponse = AssetRequestResponse.builder().requester("Google").build();

        traderWebClient.post()
                .uri(SHARE_REQUESTS_URI + "/decline")
                .header("Workflow-Id", "IT-declineShareRequest")
                .body(BodyInserters.fromValue(assetRequestResponse))
                .exchange()
                .expectStatus().isForbidden();
    }
}