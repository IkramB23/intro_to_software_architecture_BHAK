package com.bhak.project.dto;

import com.bhak.project.entity.RoleType;
import lombok.Data;

// donnees d'inscription
// si aucun role n'est precise, le niveau USER est applique
@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String phoneNumber;
    private String password;
    private RoleType roleType;
}
