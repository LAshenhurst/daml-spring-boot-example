package com.lashe.example.api.configuration.filters;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class ResponseFilter implements WebFilter {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = MDC.get("traceId");
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, traceId);
        return chain.filter(exchange);
    }
}
