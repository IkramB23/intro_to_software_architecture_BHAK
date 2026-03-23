package com.bhak.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA entity representing a user account in the {@code tbl_users} table.
 *
 * <h2>Purpose</h2>
 * Models the public information of an account: unique username, role,
 * verification status (email), and creation date.
 * Sensitive data (password, email, phone) is isolated in the
 * {@link Credentials} entity, linked via a bidirectional {@code @OneToOne}
 * relationship with full cascade ({@code CascadeType.ALL} + {@code orphanRemoval}).
 *
 * <h2>Relationships</h2>
 * <ul>
 *   <li>{@code @ManyToOne} to {@link Role} – a user has one role (ADMIN, MODERATOR, USER).</li>
 *   <li>{@code @OneToOne(mappedBy="user")} to {@link Credentials} – authentication data.
 *       Deleting the {@code User} automatically deletes its {@code Credentials}.</li>
 * </ul>
 *
 * <h2>Annotations</h2>
 * {@code @Entity}, {@code @Table}, {@code @PrePersist} (auto-populates {@code createdAt}),
 * Lombok ({@code @Data}, {@code @NoArgsConstructor}, {@code @AllArgsConstructor}).
 */
@Entity
@Table(name = "tbl_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // pseudo unique
    @Column(name = "pseudo", unique = true, nullable = false, length = 50)
    private String username;

    // role de l'utilisateur (plusieurs users peuvent avoir le meme role)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "permission_id", nullable = false)
    private Role role;

    // indique si le compte a ete verifie par e-mail
    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    // credentials lies au compte (cascade = si on supprime le user, on supprime ses credentials)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Credentials credentials;

    // date de creation du compte
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // appelee automatiquement avant le premier insert en base
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
