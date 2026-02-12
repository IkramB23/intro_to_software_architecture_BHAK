package com.bhak.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

// entite principale representant un compte utilisateur
// separee des donnees sensibles (Credentials) et du role
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
