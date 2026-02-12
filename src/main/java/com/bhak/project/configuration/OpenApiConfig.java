package com.bhak.project.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// configuration swagger / openapi
// accessible a http://localhost:8080/swagger-ui.html
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Architecture Logicielle - API BHAK",
        version = "2.0",
        description = "Système de gestion des comptes avec authentification JWT, "
                    + "CRUD administrateur et pagination.",
        contact = @Contact(name = "BHAK")
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Token JWT obtenu via POST /api/auth/login"
)
public class OpenApiConfig {
}
