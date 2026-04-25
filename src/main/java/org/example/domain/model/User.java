
package org.example.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record User(
        String id,
        String name,
        String email,
        String passwordHash,
        List<Phone> phones,
        Instant createdAt,
        Instant updatedAt,
        boolean active
) {
    public User {
        phones = List.copyOf(Optional.ofNullable(phones).orElse(List.of()));
    }
}
