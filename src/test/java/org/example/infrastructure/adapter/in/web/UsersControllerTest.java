package org.example.infrastructure.adapter.in.web;

import org.example.application.service.IdempotencyService;
import org.example.application.service.UserService;
import org.example.domain.exception.DuplicateEmailException;
import org.example.domain.exception.UserNotFoundException;
import org.example.infrastructure.config.SecurityConfig;
import org.example.users.api.model.PhoneResponse;
import org.example.users.api.model.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = UsersController.class)
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
class UsersControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockBean
  private UserService userService;

  @MockBean
  private IdempotencyService idempotencyService;

  @Test
  void shouldCreateUser() {
    UserResponse response = userResponse();
    when(idempotencyService.find("req-1")).thenReturn(Mono.empty());
    when(userService.create(any())).thenReturn(Mono.just(response));
    when(idempotencyService.save("req-1", response)).thenReturn(Mono.empty());

    webTestClient.post()
        .uri("/api/v1/users")
        .header("X-Request-Id", "req-1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "name": "Juan Perez",
              "email": "juan@mail.com",
              "password": "Abc12345",
              "phones": [
                {
                  "number": "1234567",
                  "cityCode": "1",
                  "countryCode": "57"
                }
              ]
            }
            """)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.email").isEqualTo("juan@mail.com")
        .jsonPath("$.phones[0].cityCode").isEqualTo("1");
  }

  @Test
  void shouldListUsers() {
    when(userService.findAll()).thenReturn(Flux.just(userResponse()));

    webTestClient.get()
        .uri("/api/v1/users")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$[0].name").isEqualTo("Juan Perez");
  }

  @Test
  void shouldReturnBadRequestWhenCreateRequestIsInvalid() {
    when(idempotencyService.find("req-1")).thenReturn(Mono.empty());

    webTestClient.post()
        .uri("/api/v1/users")
        .header("X-Request-Id", "req-1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "name": "Juan Perez",
              "email": "correo-invalido",
              "password": "Abc12345",
              "phones": [
                {
                  "number": "1234567",
                  "cityCode": "1",
                  "countryCode": "57"
                }
              ]
            }
            """)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
          .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
          .jsonPath("$.status").isEqualTo(400)
          .jsonPath("$.message").isEqualTo("El correo no tiene un formato válido")
        .jsonPath("$.path").isEqualTo("/api/v1/users")
        .jsonPath("$.timestamp").exists();
  }

  @Test
  void shouldReturnBadRequestWhenPasswordIsInvalid() {
    when(idempotencyService.find("req-1")).thenReturn(Mono.empty());

    webTestClient.post()
        .uri("/api/v1/users")
        .header("X-Request-Id", "req-1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "name": "Juan Perez",
              "email": "juan@mail.com",
              "password": "abc",
              "phones": [
                {
                  "number": "1234567",
                  "cityCode": "1",
                  "countryCode": "57"
                }
              ]
            }
            """)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
          .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
          .jsonPath("$.status").isEqualTo(400)
        .jsonPath("$.message").isEqualTo("La contraseña no cumple el formato requerido");
  }

  @Test
  void shouldReturnNotFoundWhenUserDoesNotExist() {
    String userId = "11111111-1111-1111-1111-111111111111";
    when(userService.findById(userId)).thenReturn(Mono.error(new UserNotFoundException(userId)));

    webTestClient.get()
        .uri("/api/v1/users/{userId}", userId)
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
          .jsonPath("$.code").isEqualTo("USER_NOT_FOUND")
          .jsonPath("$.status").isEqualTo(404)
        .jsonPath("$.message").isEqualTo("No se encontró el usuario con id: " + userId);
  }

  @Test
  void shouldReturnConflictWhenEmailAlreadyExists() {
    when(idempotencyService.find("req-1")).thenReturn(Mono.empty());
    when(userService.create(any())).thenReturn(Mono.error(new DuplicateEmailException()));

    webTestClient.post()
        .uri("/api/v1/users")
        .header("X-Request-Id", "req-1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "name": "Juan Perez",
              "email": "juan@mail.com",
              "password": "Abc12345",
              "phones": [
                {
                  "number": "1234567",
                  "cityCode": "1",
                  "countryCode": "57"
                }
              ]
            }
            """)
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
          .jsonPath("$.code").isEqualTo("DUPLICATE_EMAIL")
          .jsonPath("$.status").isEqualTo(409)
        .jsonPath("$.message").isEqualTo("El correo ya registrado");
  }

  @Test
  void shouldReturnInternalServerErrorWhenUnhandledErrorOccurs() {
    when(idempotencyService.find("req-1")).thenReturn(Mono.empty());
    when(userService.create(any())).thenReturn(Mono.error(new RuntimeException("Fallo inesperado")));

    webTestClient.post()
        .uri("/api/v1/users")
        .header("X-Request-Id", "req-1")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "name": "Juan Perez",
              "email": "juan@mail.com",
              "password": "Abc12345",
              "phones": [
                {
                  "number": "1234567",
                  "cityCode": "1",
                  "countryCode": "57"
                }
              ]
            }
            """)
        .exchange()
        .expectStatus().is5xxServerError()
        .expectBody()
          .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
          .jsonPath("$.status").isEqualTo(500)
          .jsonPath("$.message").isEqualTo("Fallo inesperado");
  }

  private UserResponse userResponse() {
    return new UserResponse(
        UUID.fromString("11111111-1111-1111-1111-111111111111"),
        "Juan Perez",
        "juan@mail.com",
        true,
        OffsetDateTime.parse("2026-04-24T20:00:00Z"),
        OffsetDateTime.parse("2026-04-24T20:00:00Z"),
        List.of(new PhoneResponse("1234567", "1", "57")));
  }
}