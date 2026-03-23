package com.bhak.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * JPA entity containing sensitive authentication data (table {@code tbl_credentials}).
 *
 * <h2>Purpose</h2>
 * Separates identification information (email, phone, hashed password)
 * from the {@link User} entity to respect the single responsibility principle
 * and limit exposure of sensitive data. The password is hashed with BCrypt
 * by {@link org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder} and is
 * never returned in JSON responses thanks to {@code @JsonProperty(Access.WRITE_ONLY)}.
 *
 * <h2>Relationships</h2>
 * {@code @OneToOne} to {@link User} (owning side, foreign key {@code account_id}).
 * The inverse link is annotated with {@code @JsonIgnore} to avoid Jackson
 * serialisation loops (User → Credentials → User → …).
 *
 * <h2>Security</h2>
 * <ul>
 *   <li>The password is never returned on read (write-only for registration).</li>
 *   <li>Email and phone are unique in the database: {@code @Column(unique=true)}.</li>
 * </ul>
 *
 * <h2>Annotations</h2>
 * {@code @Entity}, {@code @Table}, {@code @JsonProperty}, {@code @JsonIgnore},
 * Lombok ({@code @Data}, {@code @NoArgsConstructor}, {@code @AllArgsConstructor}).
 */
@Entity
@Table(name = "tbl_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Credentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // adresse mail
    @Column(name = "mail", unique = true, nullable = false, length = 100)
    private String email;

    // numero de telephone (optionnel)
    @Column(name = "tel", unique = true, nullable = true, length = 20)
    private String phoneNumber;

    // mot de passe hache, jamais renvoye dans les reponses JSON
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "secret", nullable = false)
    private String password;

    // lien vers le user, ignore en JSON pour eviter les boucles
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private User user;
}
