package com.lashe.example.api.controller;

import com.lashe.example.api.config.WebFluxTestConfig;
import com.lashe.example.api.domain.Message;
import com.lashe.example.api.domain.Role;
import com.lashe.example.api.domain.security.AuthenticationRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@ExtendWith(SpringExtension.class)
@Import(WebFluxTestConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserControllerIT {
    @Autowired
    private WebTestClient exchangeWebClient;

    @Test
    @Order(1)
    @DisplayName("Login_UnhappyPath")
    void Given_BadCredentials_When_LoggingIn_Then_ErrorReturned() {
        AuthenticationRequest authenticationRequest = AuthenticationRequest.builder()
                .username(RandomStringUtils.randomAlphanumeric(12))
                .password(RandomStringUtils.randomAlphanumeric(12))
                .build();

        exchangeWebClient.post()
                .uri("/login")
                .body(BodyInserters.fromValue(authenticationRequest))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    @Order(2)
    @DisplayName("GetRoles_HappyPath")
    void Given_AdminUser_When_GettingRoles_Then_CorrectRolesReturned() {
        Message roles = exchangeWebClient.get()
                .uri("/roles")
                .exchange()
                .expectBody(Message.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(roles);
        Assertions.assertTrue(roles.getContent().contains(Role.ADMIN.name()));
        Assertions.assertTrue(roles.getContent().contains(Role.EXCH.name()));
    }
}