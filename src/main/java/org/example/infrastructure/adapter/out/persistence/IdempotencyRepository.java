package org.example.infrastructure.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface IdempotencyRepository extends ReactiveCrudRepository<IdempotencyKey, String> {
}