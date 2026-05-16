package com.cloudcentinel.memory_management_mcp.infrastructure.rest;

import com.cloudcentinel.memory_management_mcp.application.user.RegisterOrGetUser;
import com.cloudcentinel.memory_management_mcp.application.user.RegisterOrGetUserHandler;
import com.cloudcentinel.memory_management_mcp.domain.user.entity.User;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final RegisterOrGetUserHandler handler;

    public UserRestController(RegisterOrGetUserHandler handler) {
        this.handler = handler;
    }

    public record RegisterRequest(String gitUsername) {}
    public record UserResponse(String userId, String gitUsername, boolean active) {}

    @PostMapping("/register")
    public Mono<UserResponse> register(@RequestBody RegisterRequest req) {
        return Mono.fromCallable(() -> {
            User user = handler.handle(new RegisterOrGetUser.Command(req.gitUsername()));
            return new UserResponse(user.id().value().toString(), user.gitUsername().value(), user.isActive());
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
