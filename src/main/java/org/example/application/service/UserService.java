package org.example.application.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.exception.DuplicateEmailException;
import org.example.domain.exception.InvalidPasswordException;
import org.example.domain.exception.UserNotFoundException;
import org.example.domain.model.Phone;
import org.example.domain.model.User;
import org.example.domain.port.PasswordEncoderPort;
import org.example.domain.port.UserRepositoryPort;
import org.example.infrastructure.config.SecurityProperties;
import org.example.users.api.model.PhoneRequest;
import org.example.users.api.model.PhoneResponse;
import org.example.users.api.model.UserCreateRequest;
import org.example.users.api.model.UserResponse;
import org.example.users.api.model.UserUpdateRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final SecurityProperties securityProperties;

    public Mono<UserResponse> create(UserCreateRequest request) {
        validatePassword(request.getPassword());

        Instant now = Instant.now();
        User user = new User(
                UUID.randomUUID().toString(),
                request.getName().trim(),
                normalizeEmail(request.getEmail()),
                passwordEncoder.encode(request.getPassword()),
                mapPhones(request.getPhones()),
                now,
                now,
                true
        );

        return userRepository.existsByEmail(user.email())
                .flatMap(exists -> exists
                        ? Mono.error(new DuplicateEmailException())
                        : userRepository.save(user))
                .map(this::toResponse);
    }

    public Flux<UserResponse> findAll() {
        return userRepository.findAll()
                .map(this::toResponse);
    }

    public Mono<UserResponse> findById(String userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .map(this::toResponse);
    }

    public Mono<UserResponse> update(String userId, UserUpdateRequest request) {
        validatePassword(request.getPassword());
        String normalizedEmail = normalizeEmail(request.getEmail());

        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .flatMap(existing -> ensureEmailAvailable(normalizedEmail, userId)
                        .then(userRepository.save(new User(
                                existing.id(),
                                request.getName().trim(),
                                normalizedEmail,
                                passwordEncoder.encode(request.getPassword()),
                                mapPhones(request.getPhones()),
                                existing.createdAt(),
                                Instant.now(),
                                Optional.ofNullable(request.getActive()).orElse(existing.active())
                        ))))
                .map(this::toResponse);
    }

    public Mono<Void> deleteById(String userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(userId)))
                .flatMap(user -> userRepository.deleteById(user.id()));
    }

    private Mono<Void> ensureEmailAvailable(String email, String currentUserId) {
        return userRepository.findByEmail(email)
                .filter(found -> !found.id().equals(currentUserId))
                .flatMap(found -> Mono.error(new DuplicateEmailException()))
                .then();
    }

    private List<Phone> mapPhones(List<PhoneRequest> phones) {
        return Optional.ofNullable(phones)
                .orElse(List.of())
                .stream()
                .map(phone -> new Phone(phone.getNumber(), phone.getCityCode(), phone.getCountryCode()))
                .toList();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                UUID.fromString(user.id()),
                user.name(),
                user.email(),
                user.active(),
                OffsetDateTime.ofInstant(user.createdAt(), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(user.updatedAt(), ZoneOffset.UTC),
                user.phones().stream()
                        .map(this::toPhoneResponse)
                        .toList()
        );
    }

    private PhoneResponse toPhoneResponse(Phone phone) {
        return new PhoneResponse(phone.number(), phone.cityCode(), phone.countryCode());
    }

    private String normalizeEmail(String email) {
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(String::toLowerCase)
                .orElseThrow(() -> new IllegalArgumentException("El correo es obligatorio"));
    }

    private void validatePassword(String password) {
        String regex = Optional.ofNullable(securityProperties.getPasswordRegex())
                .filter(value -> !value.isBlank())
                .orElseThrow(InvalidPasswordException::new);

        if (!Optional.ofNullable(password).orElse("").matches(regex)) {
            throw new InvalidPasswordException();
        }
    }
}