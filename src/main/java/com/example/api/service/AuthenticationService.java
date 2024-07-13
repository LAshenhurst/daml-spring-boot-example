package com.example.api.service;

import com.example.api.domain.Role;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AuthenticationService {
    <T> Mono<T> checkAdminPermissionAndContinue(Mono<T> continueFlow);

    <T> Flux<T> checkAdminPermissionAndContinue(Flux<T> continueFlow);

    Flux<Role> getRoles();

    Mono<Boolean> checkUserRole(Role expectedType);

    <T> Mono<T> checkUserRoleAndContinue(Role expectedType, Mono<T> continueFlow);
}
