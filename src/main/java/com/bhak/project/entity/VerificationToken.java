package com.bhak.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity representing an email verification token (table {@code tbl_verification_tokens}).
 *
 * <h2>Purpose</h2>
 * Verifies that a user actually owns the email address provided during
 * registration. The cleartext token (UUID) is sent by email while only its
 * BCrypt hash ({@code tokenHash}) is stored in the database — the cleartext token
 * is never persisted, ensuring security even in case of a database leak.
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>{@code VerificationService} generates a {@code tokenId} (short identifier) and a
 *       cleartext token (UUID), hashes the latter with BCrypt, then stores the object in the database.</li>
 *   <li>A link {@code /api/auth/verify?tokenId=...&t=...} is sent by email through
 *       the notification-service (RabbitMQ → MailHog).</li>
 *   <li>When clicked, the service compares the received cleartext token with the stored hash
 *       ({@code passwordEncoder.matches()}) and checks expiration ({@code expiresAt}).</li>
 *   <li>On success, the token is deleted (single-use, aka "one-shot").</li>
 * </ol>
 *
 * <h2>Annotations</h2>
 * {@code @Entity}, {@code @Table}, {@code @Id} (no auto-increment, business identifier),
 * Lombok ({@code @Data}, {@code @NoArgsConstructor}, {@code @AllArgsConstructor}).
 */
@Entity
@Table(name = "tbl_verification_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    @Id
    @Column(name = "token_id", nullable = false, length = 64)
    private String tokenId;

    // identifiant du compte associe
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // hash BCrypt du token clair
    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    // date d'expiration du token
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
