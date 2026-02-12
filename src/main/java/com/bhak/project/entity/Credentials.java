package com.bhak.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// donnees d'authentification separees du user pour isoler les infos sensibles
// le mot de passe est hache en BCrypt et jamais retourne en JSON
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
