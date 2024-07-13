package com.example.api.controller;

import com.example.api.config.WebFluxTestConfig;
import com.example.api.domain.daml.Transaction;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

@ExtendWith(SpringExtension.class)
@Import(WebFluxTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionControllerIT {
    @Autowired
    private WebTestClient traderWebClient;

    @Autowired
    private WebTestClient exchangeWebClient;

    private static final String TRANSACTIONS_URI = "/transactions";

    @Test
    @Order(1)
    @DisplayName("GetTransactions_HappyPath")
    void Given_AdminUser_When_GettingTransactions_Then_TransactionsReturned() {
        List<Transaction> transactions = exchangeWebClient.get()
                .uri(TRANSACTIONS_URI)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Transaction.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(transactions);
        Assertions.assertFalse(transactions.isEmpty());
    }

    @Test
    @Order(2)
    @DisplayName("GetTransactions_PermissionDenied")
    void Given_TraderUser_When_GettingTransactions_Then_PermissionDenied() {
        traderWebClient.get()
                .uri(TRANSACTIONS_URI)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @Order(3)
    @DisplayName("GetTransactions_BadOffset")
    void Given_BadOffset_When_GettingTransactions_Then_ErrorReturned() {
        exchangeWebClient.get()
                .uri(TRANSACTIONS_URI + "/beginOffset=" + RandomStringUtils.randomAlphanumeric(12))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(4)
    @DisplayName("GetTransactionById_HappyPath")
    void Given_ValidTransactionId_When_GettingTransactionById_Then_TransactionReturned() {
        List<Transaction> transactions = exchangeWebClient.get()
                .uri(TRANSACTIONS_URI)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Transaction.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(transactions);
        Assertions.assertFalse(transactions.isEmpty());

        Transaction transaction = exchangeWebClient.get()
                .uri(TRANSACTIONS_URI + "/" + transactions.get(0).getTransactionId())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(Transaction.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(transaction);
        Assertions.assertEquals(transactions.get(0), transaction);
    }

    @Test
    @Order(5)
    @DisplayName("GetTransactionById_PermissionDenied")
    void Given_TraderUser_When_GettingTransactionById_Then_PermissionDenied() {
        List<Transaction> transactions = exchangeWebClient.get()
                .uri(TRANSACTIONS_URI)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(Transaction.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(transactions);
        Assertions.assertFalse(transactions.isEmpty());

        traderWebClient.get()
                .uri(TRANSACTIONS_URI + "/" + transactions.get(0).getTransactionId())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @Order(6)
    @DisplayName("GetTransactionById_BadId")
    void Given_BadIdentifier_When_GettingTransactionById_Then_ErrorReturned() {
        exchangeWebClient.get()
                .uri(TRANSACTIONS_URI + "/" + RandomStringUtils.randomAlphanumeric(12))
                .exchange()
                .expectStatus().isNotFound();
    }
}