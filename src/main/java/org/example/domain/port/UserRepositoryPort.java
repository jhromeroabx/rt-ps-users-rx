
package org.example.domain.port;

import org.example.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepositoryPort {
    Mono<Boolean> existsByEmail(String email);

    Mono<User> findByEmail(String email);

    Mono<User> findById(String userId);

    Flux<User> findAll();

    Mono<User> save(User user);

    Mono<Void> deleteById(String userId);
}
