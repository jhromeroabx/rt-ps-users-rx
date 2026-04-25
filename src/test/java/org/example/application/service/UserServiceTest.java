package org.example.application.service;

import org.example.domain.exception.DuplicateEmailException;
import org.example.domain.exception.UserNotFoundException;
import org.example.domain.model.Phone;
import org.example.domain.model.User;
import org.example.domain.port.PasswordEncoderPort;
import org.example.domain.port.UserRepositoryPort;
import org.example.infrastructure.config.SecurityProperties;
import org.example.users.api.model.PhoneRequest;
import org.example.users.api.model.UserCreateRequest;
import org.example.users.api.model.UserUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private static final String USER_ID_1 = "11111111-1111-1111-1111-111111111111";
    private static final String USER_ID_2 = "22222222-2222-2222-2222-222222222222";

    private UserRepositoryPort userRepository;
    private PasswordEncoderPort passwordEncoder;
    private SecurityProperties securityProperties;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepositoryPort.class);
        passwordEncoder = mock(PasswordEncoderPort.class);
        securityProperties = new SecurityProperties();
        securityProperties.setPasswordRegex("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$");
        userService = new UserService(userRepository, passwordEncoder, securityProperties);
    }

    @Test
    void shouldCreateUserSuccessfully() {
        UserCreateRequest request = new UserCreateRequest("Juan Perez", "Juan@Mail.com", "Abc12345", List.of(phoneRequest()));
        when(userRepository.existsByEmail("juan@mail.com")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("Abc12345")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(userService.create(request))
                .assertNext(response -> {
                    org.junit.jupiter.api.Assertions.assertEquals("juan@mail.com", response.getEmail());
                    org.junit.jupiter.api.Assertions.assertEquals("Juan Perez", response.getName());
                    org.junit.jupiter.api.Assertions.assertTrue(response.getActive());
                    org.junit.jupiter.api.Assertions.assertEquals(1, response.getPhones().size());
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateEmailOnCreate() {
        UserCreateRequest request = new UserCreateRequest("Juan Perez", "juan@mail.com", "Abc12345", List.of(phoneRequest()));
        when(userRepository.existsByEmail("juan@mail.com")).thenReturn(Mono.just(true));

        StepVerifier.create(userService.create(request))
                .expectError(DuplicateEmailException.class)
                .verify();
    }

    @Test
    void shouldReturnUserById() {
        when(userRepository.findById(USER_ID_1)).thenReturn(Mono.just(savedUser(USER_ID_1, "user@mail.com")));

        StepVerifier.create(userService.findById(USER_ID_1))
                .assertNext(response -> org.junit.jupiter.api.Assertions.assertEquals("user@mail.com", response.getEmail()))
                .verifyComplete();
    }

    @Test
    void shouldFailWhenUserDoesNotExist() {
        when(userRepository.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(userService.findById("missing"))
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void shouldUpdateUser() {
        UserUpdateRequest request = new UserUpdateRequest("Jane Doe", "jane@mail.com", "Abc12345", List.of(phoneRequest()));
        request.setActive(false);

        when(userRepository.findById(USER_ID_1)).thenReturn(Mono.just(savedUser(USER_ID_1, "user@mail.com")));
        when(userRepository.findByEmail("jane@mail.com")).thenReturn(Mono.empty());
        when(passwordEncoder.encode("Abc12345")).thenReturn("updated-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(userService.update(USER_ID_1, request))
                .assertNext(response -> {
                    org.junit.jupiter.api.Assertions.assertEquals("jane@mail.com", response.getEmail());
                    org.junit.jupiter.api.Assertions.assertFalse(response.getActive());
                })
                .verifyComplete();
    }

    @Test
    void shouldDeleteExistingUser() {
        when(userRepository.findById(USER_ID_1)).thenReturn(Mono.just(savedUser(USER_ID_1, "user@mail.com")));
        when(userRepository.deleteById(USER_ID_1)).thenReturn(Mono.empty());

        StepVerifier.create(userService.deleteById(USER_ID_1))
                .verifyComplete();

        verify(userRepository).deleteById(USER_ID_1);
    }

    @Test
    void shouldListUsers() {
        when(userRepository.findAll()).thenReturn(Flux.just(savedUser(USER_ID_1, "first@mail.com"), savedUser(USER_ID_2, "second@mail.com")));

        StepVerifier.create(userService.findAll())
                .expectNextCount(2)
                .verifyComplete();
    }

    private PhoneRequest phoneRequest() {
        return new PhoneRequest("1234567", "1", "57");
    }

    private User savedUser(String id, String email) {
        Instant now = Instant.now();
        return new User(
                id,
                "Stored User",
                email,
                "hashed-password",
                List.of(new Phone("1234567", "1", "57")),
                now,
                now,
                true
        );
    }
}