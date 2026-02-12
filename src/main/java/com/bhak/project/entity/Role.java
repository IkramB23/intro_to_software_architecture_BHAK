package com.bhak.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// entite qui represente un role (ADMIN, MODERATOR, USER)
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
