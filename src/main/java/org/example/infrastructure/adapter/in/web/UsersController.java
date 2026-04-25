
package org.example.infrastructure.adapter.in.web;

import jakarta.validation.Valid;
import org.example.application.service.IdempotencyService;
import org.example.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.example.users.api.UsersApi;
import org.example.users.api.model.UserCreateRequest;
import org.example.users.api.model.UserResponse;
import org.example.users.api.model.UserUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class UsersController implements UsersApi {

    private final UserService userService;
    private final IdempotencyService idempotencyService;

    @Override
    public Mono<ResponseEntity<UserResponse>> createUser(
            @Valid @RequestBody Mono<UserCreateRequest> requestMono,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            final ServerWebExchange exchange) {

        return idempotencyService.find(requestId)
                .map(response -> ResponseEntity.ok()
                        .header("X-Idempotent-Replay", "true")
                        .body(response))
                .switchIfEmpty(requestMono
                        .flatMap(userService::create)
                        .flatMap(response -> idempotencyService.save(requestId, response)
                                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(response))));
    }

    @Override
    public Mono<ResponseEntity<Flux<UserResponse>>> listUsers(final ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(userService.findAll()));
    }

    @Override
    public Mono<ResponseEntity<UserResponse>> getUserById(UUID userId, final ServerWebExchange exchange) {
        return userService.findById(userId.toString())
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<UserResponse>> updateUser(
            UUID userId,
            @Valid @RequestBody Mono<UserUpdateRequest> userUpdateRequest,
            final ServerWebExchange exchange) {
        return userUpdateRequest
                .flatMap(request -> userService.update(userId.toString(), request))
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> deleteUser(UUID userId, final ServerWebExchange exchange) {
        return userService.deleteById(userId.toString())
                .thenReturn(ResponseEntity.noContent().build());
    }
}