package org.example.domain.port;

import org.example.users.api.model.UserResponse;
import reactor.core.publisher.Mono;

public interface IdempotencyPort {
    Mono<UserResponse> find(String requestId);

    Mono<Void> save(String requestId, UserResponse response);
}