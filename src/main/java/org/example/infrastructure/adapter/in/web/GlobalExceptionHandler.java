package org.example.infrastructure.adapter.in.web;

import org.example.domain.exception.DuplicateEmailException;
import org.example.domain.exception.InvalidPasswordException;
import org.example.domain.exception.UserNotFoundException;
import org.example.users.api.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDuplicate(DuplicateEmailException ex, ServerWebExchange exchange) {
        return Mono.just(
                ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(buildError(ex.getMessage(), exchange))
        );
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {

        String message = ex.getFieldErrors()
                .stream()
                .findFirst()
                .map(this::mapMessage)
                .orElse("Solicitud inválida");

        return Mono.just(
                ResponseEntity
                        .badRequest()
                        .body(buildError(message, exchange))
        );
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidPassword(InvalidPasswordException ex, ServerWebExchange exchange) {
        return Mono.just(
                ResponseEntity.badRequest()
                        .body(buildError(ex.getMessage(), exchange))
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(UserNotFoundException ex, ServerWebExchange exchange) {
        return Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(buildError(ex.getMessage(), exchange))
        );
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(Exception ex, ServerWebExchange exchange) {

        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(buildError("Error interno del servidor", exchange))
        );
    }

    private String mapMessage(FieldError error) {
        return switch (error.getField()) {
            case "email" -> "El correo no tiene un formato válido";
            case "password" -> "La contraseña no cumple el formato requerido";
            case "name" -> "El nombre es obligatorio";
            case "phones" -> "Debe registrar al menos un teléfono";
            default -> "Solicitud inválida";
        };
    }

    private ErrorResponse buildError(String message, ServerWebExchange exchange) {
        return new ErrorResponse(message, exchange.getRequest().getPath().value(), OffsetDateTime.now());
    }
}