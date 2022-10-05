package com.lashe.example.api.service.impl;

import com.lashe.example.api.common.exceptions.ApiException;
import com.lashe.example.api.configuration.security.UserService;
import com.lashe.example.api.domain.Role;
import com.lashe.example.api.domain.security.ExampleUser;
import com.lashe.example.api.helper.SecurityHelper;
import com.lashe.example.api.service.AuthenticationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@AllArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserService userService;

    @Override
    public <T> Mono<T> checkAdminPermissionAndContinue(Mono<T> continueFlow) {
        return getRoles()
                .any(role -> role.equals(Role.ADMIN))
                .flatMap(isAdmin -> {
                    if (Boolean.TRUE.equals(isAdmin)) { return continueFlow; }
                    return Mono.error(new ApiException(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.getReasonPhrase()));
                });
    }

    @Override
    public <T> Flux<T> checkAdminPermissionAndContinue(Flux<T> continueFlow) {
        return getRoles()
                .any(role -> role.equals(Role.ADMIN))
                .flatMapMany(isAdmin -> {
                    if (Boolean.TRUE.equals(isAdmin)) { return continueFlow; }
                    return Flux.error(new ApiException(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.getReasonPhrase()));
                });
    }

    @Override
    public Flux<Role> getRoles() {
        return SecurityHelper.getUsername()
                .flatMap(userService::findByUsername)
                .flatMapIterable(ExampleUser::getRoles);
    }

    @Override
    public Mono<Boolean> checkUserRole(Role expectedType) {
        return getRoles().any(type -> type.equals(expectedType));
    }

    @Override
    public <T> Mono<T> checkUserRoleAndContinue(Role expectedType, Mono<T> continueFlow) {
        return checkUserRole(expectedType)
                .flatMap(isExpectedType -> {
                    if (Boolean.TRUE.equals(isExpectedType)) { return continueFlow; }
                    return Mono.error(new ApiException(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.getReasonPhrase()));
                });
    }
}
