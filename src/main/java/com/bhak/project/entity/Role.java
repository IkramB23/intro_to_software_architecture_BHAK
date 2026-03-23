package com.bhak.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * JPA entity representing an authorisation role (table {@code tbl_roles}).
 *
 * <h2>Purpose</h2>
 * Stores in the database the application roles defined in the {@link RoleType} enum
 * (ADMIN, MODERATOR, USER). Each {@link User} is linked to a {@code Role}
 * via a {@code @ManyToOne} relationship (multiple users can share the same role).
 *
 * <h2>Initialisation</h2>
 * Roles are automatically inserted at startup by
 * {@link com.bhak.project.configuration.DataInitializer} if they do not yet exist.
 *
 * <h2>Attributes</h2>
 * <ul>
 *   <li>{@code name} ({@link RoleType}) – technical name of the role, stored as {@code EnumType.STRING}.
 *       Unique column, used for lookups via {@code RoleRepository#findByName()}.</li>
 *   <li>{@code description} – human-readable label (e.g. "Administrator").</li>
 * </ul>
 *
 * <h2>Annotations</h2>
 * {@code @Entity}, {@code @Table}, {@code @Enumerated(EnumType.STRING)},
 * Lombok ({@code @Data}, {@code @NoArgsConstructor}, {@code @AllArgsConstructor}).
 */
@Entity
@Table(name = "tbl_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // nom du role stocke en tant que string dans la base
    @Enumerated(EnumType.STRING)
    @Column(name = "level", unique = true, nullable = false, length = 30)
    private RoleType name;

    // description lisible du role
    @Column(name = "label", length = 255)
    private String description;
}
