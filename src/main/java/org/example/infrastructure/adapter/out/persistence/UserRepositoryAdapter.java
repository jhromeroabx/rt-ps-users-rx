package org.example.infrastructure.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import org.example.domain.model.User;
import org.example.domain.model.Phone;
import org.example.domain.port.UserRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final UserRepository userRepository;

    private final PhoneRepository phoneRepository;

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return userRepository.findByEmail(email)
                .flatMap(this::mapToDomain);
    }

    @Override
    public Mono<User> findById(String userId) {
        return userRepository.findById(userId)
                .flatMap(this::mapToDomain);
    }

    @Override
    public Flux<User> findAll() {
        return userRepository.findAll()
                .concatMap(this::mapToDomain);
    }

    @Override
    @Transactional
    public Mono<User> save(User user) {
        List<PhoneEntity> phoneEntities = mapPhones(user.id(), user.phones());
        return userRepository.existsById(user.id())
                .flatMap(exists -> {
                    UserEntity entity = mapToEntity(user, !exists);

                    return userRepository.save(entity)
                            .flatMap(savedUser -> phoneRepository.deleteAllByUserId(savedUser.getId())
                                    .thenMany(Flux.fromIterable(phoneEntities))
                                    .flatMap(phoneRepository::save)
                                    .then(Mono.just(mapToDomain(savedUser, user.phones()))));
                });
    }

    @Override
    @Transactional
    public Mono<Void> deleteById(String userId) {
        return phoneRepository.deleteAllByUserId(userId)
                .then(userRepository.deleteById(userId));
    }

    private Mono<User> mapToDomain(UserEntity entity) {
        return phoneRepository.findAllByUserId(entity.getId())
                .map(this::mapPhone)
                .collectList()
                .map(phones -> mapToDomain(entity, phones));
    }

    private User mapToDomain(UserEntity entity, List<Phone> phones) {
        return new User(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                phones,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                Boolean.TRUE.equals(entity.getActive()));
    }

    private UserEntity mapToEntity(User user, boolean isNew) {
        return UserEntity.builder()
                .id(user.id())
                .name(user.name())
                .email(user.email())
                .passwordHash(user.passwordHash())
                .active(user.active())
                .createdAt(user.createdAt())
                .updatedAt(user.updatedAt())
                .newEntity(isNew)
                .build();
    }

    private List<PhoneEntity> mapPhones(String userId, List<Phone> phones) {
        return phones.stream()
                .map(phone -> PhoneEntity.builder()
                        .userId(userId)
                        .number(phone.number())
                        .cityCode(phone.cityCode())
                        .countryCode(phone.countryCode())
                        .build())
                .toList();
    }

    private Phone mapPhone(PhoneEntity entity) {
        return new Phone(entity.getNumber(), entity.getCityCode(), entity.getCountryCode());
    }
}
