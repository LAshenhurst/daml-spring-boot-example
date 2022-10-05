package com.lashe.example.api.controller;

import com.lashe.example.api.common.exceptions.ApiException;
import com.lashe.example.api.configuration.security.JWTUtil;
import com.lashe.example.api.configuration.security.PBKDF2Encoder;
import com.lashe.example.api.configuration.security.UserService;
import com.lashe.example.api.domain.Message;
import com.lashe.example.api.domain.Role;
import com.lashe.example.api.domain.security.AuthenticationRequest;
import com.lashe.example.api.domain.security.AuthenticationResponse;
import com.lashe.example.api.domain.security.ExampleUser;
import com.lashe.example.api.helper.SecurityHelper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {
    private final JWTUtil jwtUtil;
    private final PBKDF2Encoder passwordEncoder;
    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "Log in using a username and password")
    public Mono<ResponseEntity<AuthenticationResponse>> login(@RequestBody AuthenticationRequest ar) {
        return userService.findByUsername(ar.getUsername())
                .filter(userDetails -> passwordEncoder.encode(ar.getPassword()).equals(userDetails.getPassword()))
                .map(userDetails -> ResponseEntity.ok(new AuthenticationResponse(jwtUtil.generateToken(userDetails))))
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.UNAUTHORIZED, "Unauthorized")));
    }

    @PostMapping("/decode-jwt/{jwt}")
    @Operation(summary = "Decode a given jwt to view the Claims contained")
    public Jws<Claims> decode(@PathVariable String jwt) {
        return jwtUtil.decodeToken(jwt);
    }

    @GetMapping("/roles")
    @Operation(summary = "Get all roles associated with the logged in user")
    public Mono<Message> getRoles() {
        return SecurityHelper.getUsername()
                .flatMap(userService::findByUsername)
                .flatMapIterable(ExampleUser::getRoles)
                .map(Role::name)
                .collectList()
                .map(roles -> new Message(String.join(", ", roles)));
    }
}
