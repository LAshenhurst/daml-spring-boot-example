package com.example.api.config;

import com.example.api.configuration.security.JWTUtil;
import com.example.api.configuration.security.UserService;
import com.example.api.domain.security.ExampleUser;
import com.google.common.net.HttpHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

@Configuration
@RequiredArgsConstructor
public class WebFluxTestConfig {
    private final JWTUtil jwtUtil;
    private final UserService userService;

    @Bean
    public WebTestClient traderWebClient(ApplicationContext applicationContext) {
        ExampleUser trader = userService.findByUsername("Alice").block();
        String jwt = jwtUtil.generateToken(trader);
        return createClient(applicationContext, jwt);
    }

    @Bean
    public WebTestClient corporationWebClient(ApplicationContext applicationContext) {
        ExampleUser corporation = userService.findByUsername("Google").block();
        String jwt = jwtUtil.generateToken(corporation);
        return createClient(applicationContext, jwt);
    }

    @Bean
    public WebTestClient exchangeWebClient(ApplicationContext applicationContext) {
        ExampleUser exchange = userService.findByUsername("NASDAQ").block();
        String jwt = jwtUtil.generateToken(exchange);
        return createClient(applicationContext, jwt);
    }

    private WebTestClient createClient(ApplicationContext applicationContext, String jwt) {
        return WebTestClient.bindToApplicationContext(applicationContext).configureClient()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .build();
    }
}
