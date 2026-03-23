package com.bhak.project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API health-check controller.
 *
 * <h2>Purpose</h2>
 * Exposes a single endpoint {@code GET /} accessible without authentication.
 * It returns a JSON object containing the application name, version,
 * status ({@code UP}), a timestamp, and the list of main routes.
 *
 * <h2>How it works</h2>
 * No database calls or external dependencies: the endpoint responds
 * immediately with static information, allowing quick verification
 * that the service is running and reachable (useful for Docker
 * or a load-balancer).
 *
 * <h2>Technologies</h2>
 * {@code @RestController}, Swagger/OpenAPI ({@code @Tag}, {@code @Operation}).
 */
@RestController
@Tag(name = "Santé", description = "Vérification de l'état de l'API")
public class HomeController {

    @GetMapping("/")
    @Operation(summary = "État de l'API")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application", "Architecture Logicielle - Projet BHAK");
        info.put("version", "2.0.0");
        info.put("status", "UP");
        info.put("timestamp", LocalDateTime.now().toString());
        info.put("routes", Map.of(
            "inscription", "POST /api/auth/register",
            "connexion",   "POST /api/auth/login",
            "admin_users", "GET  /api/admin/users"
        ));
        return ResponseEntity.ok(info);
    }
}
