package com.lashe.example.api.configuration.security;

import com.lashe.example.api.domain.Role;
import com.lashe.example.api.domain.security.ExampleUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.*;

@Service
public class UserService {

    private Map<String, ExampleUser> users;

    @PostConstruct
    public void init() {
        users = new HashMap<>();
        //password is password
        final String password = "Y/zMAg4P07PpGZLiyWutYveUz3f8TV1S0kMlGxWG4o0=";

        users.put("NASDAQ", new ExampleUser("NASDAQ", password, Set.of(Role.EXCH, Role.ADMIN)));
        users.put("Alice", new ExampleUser("Alice", password, Collections.singleton(Role.TRDR)));
        users.put("Google", new ExampleUser("Google", password, Collections.singleton(Role.CORP)));
    }

    public Mono<ExampleUser> findByUsername(String username) {
        return Mono.justOrEmpty(users.get(username));
    }
}