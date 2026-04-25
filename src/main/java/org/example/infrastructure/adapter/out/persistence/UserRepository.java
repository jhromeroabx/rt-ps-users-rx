
package org.example.infrastructure.adapter.out.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<UserEntity, String> {
    Mono<Boolean> existsByEmail(String email);

    Mono<UserEntity> findByEmail(String email);
}
