package com.lashe.example.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lashe.example.api.common.filters.ColumnFilter;
import com.lashe.example.api.common.filters.impl.NumberColumnFilter;
import com.lashe.example.api.common.filters.impl.TextColumnFilter;
import com.lashe.example.api.domain.Role;
import com.lashe.example.api.domain.asset.AssetRequestResponse;
import com.lashe.example.api.domain.asset.cash.CreateCashRequest;
import com.lashe.example.api.domain.asset.share.CreateShareRequest;
import com.lashe.example.api.domain.daml.CreateLedgerParty;
import com.lashe.example.api.domain.exchange.BuyOffer;
import com.lashe.example.api.domain.exchange.MatchOffers;
import com.lashe.example.api.domain.exchange.Offer;
import com.lashe.example.api.domain.exchange.SellOffer;
import com.lashe.example.api.domain.rest.request.FilterRequest;
import com.lashe.example.api.domain.rest.response.FilterResponse;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.Assertions;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;

public class TestUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static <T> List<T> getFilterResponse(WebTestClient webTestClient, FilterRequest filterRequest, String URI, Class<T> returnClass) {
        FilterResponse<LinkedHashMap<String, Object>> filterResponse = webTestClient.post()
                .uri(URI)
                .body(BodyInserters.fromValue(filterRequest))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(new ParameterizedTypeReference<FilterResponse<LinkedHashMap<String, Object>>>() { })
                .returnResult().getResponseBody();

        Assertions.assertNotNull(filterResponse);
        Assertions.assertNotNull(filterResponse.getData());

        return filterResponse.getData().stream()
                .map(obj -> OBJECT_MAPPER.convertValue(obj, returnClass))
                .collect(Collectors.toList());
    }

    public static void awaitUntilAsserted(ThrowingRunnable throwingRunnable) {
        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofSeconds(2))
                .untilAsserted(throwingRunnable);
    }

    public static void createCashRequest(WebTestClient webTestClient, String currency, Double amount) {
        CreateCashRequest createCashRequest = CreateCashRequest.builder()
                .amount(amount)
                .currency(Currency.getInstance(currency))
                .build();

        webTestClient.post()
                .uri("/corporation/cash-request")
                .header("Workflow-Id", "IT-createCashRequest")
                .body(BodyInserters.fromValue(createCashRequest))
                .exchange();
    }

    public static void createShareRequest(WebTestClient corpWebClient, Double amount) {
        CreateShareRequest createShareRequest = CreateShareRequest.builder().value(amount).build();
        corpWebClient.post()
                .uri("/corporation/share-request")
                .header("Workflow-Id", "IT-createShareRequest")
                .body(BodyInserters.fromValue(createShareRequest))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    public static void acceptCashRequest(WebTestClient exchangeWebClient, String currency) {
        acceptCashRequest(exchangeWebClient, currency, null);
    }

    public static void acceptCashRequest(WebTestClient exchangeWebClient, String currency, String requestingParty) {
        String requester = StringUtils.isEmpty(requestingParty) ? "Google" : requestingParty;
        AssetRequestResponse assetRequestResponse = AssetRequestResponse.builder()
                .currency(Currency.getInstance(currency))
                .requester(requester)
                .build();

        exchangeWebClient.post()
                .uri("/cash/requests/accept")
                .header("Workflow-Id", "IT-acceptCashRequest")
                .body(BodyInserters.fromValue(assetRequestResponse))
                .exchange();
    }

    public static void acceptShareRequest(WebTestClient exchangeWebClient) {
        AssetRequestResponse assetRequestResponse = AssetRequestResponse.builder().requester("Google").build();

        exchangeWebClient.post()
                .uri("/shares/requests/accept")
                .header("Workflow-Id", "IT-acceptShareRequest")
                .body(BodyInserters.fromValue(assetRequestResponse))
                .exchange();
    }

    public static void createCash(WebTestClient exchangeWebClient, WebTestClient requesterClient, String requestingParty, Role requesterRole, CreateCashRequest createCashRequest) {
        String uri = requesterRole.equals(Role.CORP) ? "/corporation/cash-request" : "/trader/cash-request";
        requesterClient.post()
                .uri(uri)
                .header("Workflow-Id", "IT-createCashRequest")
                .body(BodyInserters.fromValue(createCashRequest))
                .exchange()
                .expectStatus().is2xxSuccessful();

        acceptCashRequest(exchangeWebClient, createCashRequest.getCurrency().getCurrencyCode(), requestingParty);
    }

    public static void createShares(WebTestClient exchangeWebClient, WebTestClient requesterClient, CreateShareRequest createShareRequest) {
        requesterClient.post()
                .uri("/corporation/share-request")
                .header("Workflow-Id", "IT-createShareRequest")
                .body(BodyInserters.fromValue(createShareRequest))
                .exchange()
                .expectStatus().is2xxSuccessful();

        acceptShareRequest(exchangeWebClient);
    }

    public static void createBuyOffer(WebTestClient webTestClient, Role buyerRole, Offer offer) {
        String uri = buyerRole.equals(Role.TRDR) ? "/trader/buy-offer" : "/corporation/buy-offer";
        String workflowId = buyerRole.equals(Role.TRDR) ? "IT-createTraderBuyOffer" : "IT-createCorpBuyOffer";

        webTestClient.post()
                .uri(uri)
                .header("Workflow-Id", workflowId)
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    public static void createSellOffer(WebTestClient webTestClient, Role sellerRole, Offer offer) {
        String uri = sellerRole.equals(Role.TRDR) ? "/trader/sell-offer" : "/corporation/sell-offer";
        String workflowId = sellerRole.equals(Role.TRDR) ? "IT-createTraderBuyOffer" : "IT-createCorpBuyOffer";

        webTestClient.post()
                .uri(uri)
                .header("Workflow-Id", workflowId)
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }
    
    public static void cancelBuyOffers(WebTestClient webTestClient, String buyer) {
        TextColumnFilter buyerFilter = new TextColumnFilter("equals", buyer);
        FilterRequest filterRequest = FilterRequest.builder().filterModel(Collections.singletonMap("buyer", buyerFilter)).build();
        List<BuyOffer> buyOffers = getFilterResponse(webTestClient, filterRequest, "/exchange/buy/filters", BuyOffer.class);
        
        buyOffers.forEach(buyOffer ->
                webTestClient.delete()
                        .uri("/exchange/buy/" + buyOffer.getIdentifier())
                        .header("Workflow-Id", "IT-cancelBuyOffer")
                        .exchange()
                        .expectStatus().is2xxSuccessful()
        );
    }

    public static void cancelSellOffers(WebTestClient webTestClient, String seller) {
        TextColumnFilter sellerFilter = new TextColumnFilter("equals", seller);
        FilterRequest filterRequest = FilterRequest.builder().filterModel(Collections.singletonMap("seller", sellerFilter)).build();
        List<SellOffer> sellOffers = getFilterResponse(webTestClient, filterRequest, "/exchange/sell/filters", SellOffer.class);

        sellOffers.forEach(buyOffer ->
                webTestClient.delete()
                        .uri("/exchange/sell/" + buyOffer.getIdentifier())
                        .header("Workflow-Id", "IT-cancelBuyOffer")
                        .exchange()
                        .expectStatus().is2xxSuccessful()
        );
    }

    public static void createMatchingBuyAndSellOffers(WebTestClient trader, WebTestClient corporation, String currency, Double amount, Double pricePer) {
        Offer offer = Offer.builder()
                .currency(currency)
                .pricePerShare(pricePer)
                .amount(BigDecimal.valueOf(amount))
                .corp("Google")
                .build();

        trader.post()
                .uri("/trader/buy-offer")
                .header("Workflow-Id", "IT-createTraderBuyOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is2xxSuccessful();

        corporation.post()
                .uri("/corporation/sell-offer")
                .header("Workflow-Id", "IT-createCorpSellOffer")
                .body(BodyInserters.fromValue(offer))
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    public static void createParty(WebTestClient exchangeWebClient, String partyName) {
        CreateLedgerParty createLedgerParty = CreateLedgerParty.builder()
                .displayName(partyName)
                .identifierHint(partyName)
                .build();

        exchangeWebClient.post()
                .uri("/ledger-management/parties")
                .body(BodyInserters.fromValue(createLedgerParty))
                .header("Workflow-Id", "IT-createLedgerParty")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    public static void tradeShares(WebTestClient exchange, WebTestClient trader, WebTestClient corp, Double amount, Double pricePer, String currency) {
        CreateCashRequest createCashRequest = CreateCashRequest.builder()
                .currency(Currency.getInstance(currency))
                .amount(amount * pricePer)
                .build();
        createCash(exchange, trader, "Alice", Role.TRDR, createCashRequest);
        createShares(exchange, corp, CreateShareRequest.builder().value(amount).build());
        createMatchingBuyAndSellOffers(trader, corp, currency, amount, pricePer);

        TextColumnFilter corpFilter = new TextColumnFilter("equals", "Google");
        NumberColumnFilter amountFilter = new NumberColumnFilter("equals", amount, null);
        NumberColumnFilter priceFilter = new NumberColumnFilter("equals", pricePer, null);
        Map<String, ColumnFilter> filterModel = Map.of(
                "corporation", corpFilter,
                "amount", amountFilter,
                "price", priceFilter
        );

        List<BuyOffer> buyOffers = getFilterResponse(exchange, FilterRequest.builder().filterModel(filterModel).build(), "/exchange/buy/filters", BuyOffer.class);
        List<SellOffer> sellOffers = getFilterResponse(exchange, FilterRequest.builder().filterModel(filterModel).build(), "/exchange/sell/filters", SellOffer.class);

        Assertions.assertEquals(1, buyOffers.size());
        Assertions.assertEquals(1, sellOffers.size());

        MatchOffers matchOffers = MatchOffers.builder()
                .sellOfferIdentifier(sellOffers.get(0).getIdentifier())
                .buyOfferIdentifier(buyOffers.get(0).getIdentifier())
                .build();

        exchange.post()
                .uri("/custodian/match")
                .body(BodyInserters.fromValue(matchOffers))
                .header("Workflow-Id", "IT-matchOffers")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }
}