package org.example.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table(name = "idempotency_key")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey implements Persistable<String> {

    @Id
    @Column("request_id")
    private String requestId;

    @Column("response_body")
    private String responseBody;

    @Column("created_at")
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean newEntity = false;

    @Override
    public String getId() {
        return requestId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }
}