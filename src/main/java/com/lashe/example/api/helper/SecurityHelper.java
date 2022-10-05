package com.lashe.example.api.helper;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

import java.security.Principal;

public final class SecurityHelper {
    public static Mono<String> getUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(Principal::getName);
    }

    private SecurityHelper() { throw new IllegalStateException("Utility class."); }
}