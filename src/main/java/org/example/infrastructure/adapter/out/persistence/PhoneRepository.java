package org.example.infrastructure.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PhoneRepository extends ReactiveCrudRepository<PhoneEntity, Long> {
    Flux<PhoneEntity> findAllByUserId(String userId);

    Mono<Void> deleteAllByUserId(String userId);
}