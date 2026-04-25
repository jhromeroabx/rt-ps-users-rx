package org.example.application.service;

import org.example.domain.port.IdempotencyPort;
import org.example.users.api.model.PhoneResponse;
import org.example.users.api.model.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IdempotencyServiceTest {

    private IdempotencyPort idempotencyPort;
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyPort = mock(IdempotencyPort.class);
        idempotencyService = new IdempotencyService(idempotencyPort);
    }

    @Test
    void shouldFindStoredResponseWhenRequestIdHasText() {
        UserResponse response = userResponse();
        when(idempotencyPort.find("req-1")).thenReturn(Mono.just(response));

        StepVerifier.create(idempotencyService.find("req-1"))
                .expectNext(response)
                .verifyComplete();

        verify(idempotencyPort).find("req-1");
    }

    @Test
    void shouldReturnEmptyWhenFindRequestIdIsNull() {
        StepVerifier.create(idempotencyService.find(null))
                .verifyComplete();

        verifyNoInteractions(idempotencyPort);
    }

    @Test
    void shouldReturnEmptyWhenFindRequestIdIsBlank() {
        StepVerifier.create(idempotencyService.find("   "))
                .verifyComplete();

        verifyNoInteractions(idempotencyPort);
    }

    @Test
    void shouldSaveResponseWhenRequestIdHasText() {
        UserResponse response = userResponse();
        when(idempotencyPort.save("req-1", response)).thenReturn(Mono.empty());

        StepVerifier.create(idempotencyService.save("req-1", response))
                .verifyComplete();

        verify(idempotencyPort).save("req-1", response);
    }

    @Test
    void shouldReturnEmptyWhenSaveRequestIdIsNull() {
        StepVerifier.create(idempotencyService.save(null, userResponse()))
                .verifyComplete();

        verifyNoInteractions(idempotencyPort);
    }

    @Test
    void shouldReturnEmptyWhenSaveRequestIdIsBlank() {
        StepVerifier.create(idempotencyService.save(" ", userResponse()))
                .verifyComplete();

        verifyNoInteractions(idempotencyPort);
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