package com.banenor.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("verification_tokens")
public class VerificationToken {
    @Id
    private Long id;
    private String token;
    private Long userId;
    private Instant expiresAt;
    private Instant createdAt;
}
