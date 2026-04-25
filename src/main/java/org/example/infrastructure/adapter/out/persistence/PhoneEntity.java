package org.example.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "phones")
public class PhoneEntity {
    @Id
    private Long id;
    @Column("user_id")
    private String userId;
    private String number;
    @Column("city_code")
    private String cityCode;
    @Column("country_code")
    private String countryCode;
}