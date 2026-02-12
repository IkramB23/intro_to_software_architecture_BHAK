package com.bhak.project.controller;

import com.bhak.project.configuration.JwtUtils;
import com.bhak.project.dto.LoginRequest;
import com.bhak.project.dto.RegisterRequest;
import com.bhak.project.entity.Credentials;
import com.bhak.project.entity.Role;
import com.bhak.project.entity.RoleType;
import com.bhak.project.entity.User;
import com.bhak.project.exception.DuplicateResourceException;
import com.bhak.project.exception.InvalidRequestException;
import com.bhak.project.repository.CredentialsRepository;
import com.bhak.project.repository.RoleRepository;
import com.bhak.project.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

// endpoints publics : inscription et connexion
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentification", description = "Inscription et connexion")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    // ---- inscription ----

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un nouveau compte")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Compte créé"),
        @ApiResponse(responseCode = "400", description = "Données manquantes"),
        @ApiResponse(responseCode = "409", description = "Pseudo / mail / tel déjà pris")
    })
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        log.info("Inscription demandée pour [{}]", request.getUsername());

        // Validation des champs obligatoires
        if (request.getUsername() == null || request.getEmail() == null || request.getPassword() == null) {
            throw new InvalidRequestException("Les champs pseudo, mail et mot de passe sont obligatoires");
        }

        // Vérifications d'unicité
        if (userRepository.findByUsername(request.getUsername()) != null) {
            throw new DuplicateResourceException("pseudo", request.getUsername());
        }
        if (credentialsRepository.findByEmail(request.getEmail()) != null) {
            throw new DuplicateResourceException("mail", request.getEmail());
        }
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (credentialsRepository.findByPhoneNumber(request.getPhoneNumber()) != null) {
                throw new DuplicateResourceException("tel", request.getPhoneNumber());
            }
        }

        // Rôle par défaut : USER
        RoleType type = (request.getRoleType() != null) ? request.getRoleType() : RoleType.USER;
        Role role = roleRepository.findByName(type);
        if (role == null) {
            throw new InvalidRequestException("Le niveau d'autorisation '" + type + "' n'est pas référencé");
        }

        // Construction de l'agrégat User + Credentials
        User user = new User();
        user.setUsername(request.getUsername());
        user.setRole(role);

        Credentials cred = new Credentials();
        cred.setEmail(request.getEmail());
        cred.setPhoneNumber(request.getPhoneNumber());
        cred.setPassword(passwordEncoder.encode(request.getPassword()));
        cred.setUser(user);
        user.setCredentials(cred);

        User saved = userRepository.save(user);
        log.info("Compte [{}] inscrit avec le rôle {}", saved.getUsername(), type);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ---- connexion ----

    @PostMapping("/login")
    @Operation(summary = "Connexion et obtention du token JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authentification réussie"),
        @ApiResponse(responseCode = "401", description = "Identifiants incorrects")
    })
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        log.info("Tentative de connexion [{}]", request.getUsername());

        if (request.getUsername() == null || request.getPassword() == null) {
            throw new InvalidRequestException("Le pseudo et le mot de passe sont obligatoires");
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            if (!auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants invalides");
            }

            String token = jwtUtils.generateToken(request.getUsername());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("token", token);
            body.put("type", "Bearer");

            log.info("Connexion réussie pour [{}]", request.getUsername());
            return ResponseEntity.ok(body);

        } catch (AuthenticationException ex) {
            log.warn("Échec de connexion pour [{}]", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants invalides");
        }
    }
}
