package org.example.application.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.port.IdempotencyPort;
import org.example.users.api.model.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyPort idempotencyPort;

    public Mono<UserResponse> find(String requestId) {
        return Optional.ofNullable(requestId)
                .filter(StringUtils::hasText)
                .map(idempotencyPort::find)
                .orElseGet(Mono::empty);
    }

    public Mono<Void> save(String requestId, UserResponse response) {
        return Optional.ofNullable(requestId)
                .filter(StringUtils::hasText)
                .map(id -> idempotencyPort.save(id, response))
                .orElseGet(Mono::empty);
    }
}