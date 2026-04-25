package org.example.infrastructure.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.domain.port.IdempotencyPort;
import org.example.users.api.model.UserResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class IdempotencyRepositoryAdapter implements IdempotencyPort {

        private final IdempotencyRepository repository;
        private final ObjectMapper objectMapper;

        @Override
        public Mono<UserResponse> find(String requestId) {
                return repository.findById(requestId)
                                .flatMap(entity -> Mono
                                                .fromCallable(() -> objectMapper.readValue(entity.getResponseBody(),
                                                                UserResponse.class))
                                                .subscribeOn(Schedulers.boundedElastic()));
        }

        @Override
        public Mono<Void> save(String requestId, UserResponse response) {
                return Mono.fromCallable(() -> objectMapper.writeValueAsString(response))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(serialized -> repository.save(
                                                IdempotencyKey.builder()
                                                                .requestId(requestId)
                                                                .responseBody(serialized)
                                                                .createdAt(Instant.now())
                                                                .newEntity(true)
                                                                .build()))
                                .then();
        }
}