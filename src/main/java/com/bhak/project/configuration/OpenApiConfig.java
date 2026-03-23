package com.bhak.project.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI configuration for interactive API documentation.
 *
 * <h2>Purpose</h2>
 * Automatically generates a Swagger UI interface accessible at
 * {@code http://localhost:8080/swagger-ui.html} where each endpoint is documented
 * with its parameters, return codes, and examples.
 *
 * <h2>How it works</h2>
 * <ul>
 *   <li>{@code @OpenAPIDefinition} defines the general metadata (title, version,
 *       description, contact) and adds a global {@code SecurityRequirement}
 *       requiring a Bearer token.</li>
 *   <li>{@code @SecurityScheme} declares the {@code bearerAuth} security scheme
 *       of type HTTP Bearer JWT. Swagger UI then offers an
 *       "Authorize" button to inject the token into requests.</li>
 * </ul>
 *
 * <h2>Technologies</h2>
 * SpringDoc OpenAPI (dependency {@code springdoc-openapi-starter-webmvc-ui}),
 * automatically generates the JSON at {@code /v3/api-docs} and the Swagger UI.
 */
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
