package com.bhak.project.dto;

import lombok.Data;

// donnees de connexion
@Data
public class LoginRequest {
    private String username;
    private String password;
}
