package org.example.infrastructure.adapter.in.web;

import org.example.application.service.IdempotencyService;
import org.example.application.service.UserService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = UsersController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
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
        when(idempotencyService.save(eq("req-1"), eq(response))).thenReturn(Mono.empty());

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

    private UserResponse userResponse() {
        return new UserResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Juan Perez",
                "juan@mail.com",
                true,
                OffsetDateTime.parse("2026-04-24T20:00:00Z"),
                OffsetDateTime.parse("2026-04-24T20:00:00Z"),
                List.of(new PhoneResponse("1234567", "1", "57"))
        );
    }
}