package com.bhak.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

// token de verification d'e-mail
// le token clair n'est jamais stocke : seul le hash BCrypt est persiste
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
