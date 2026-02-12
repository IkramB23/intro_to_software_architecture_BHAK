package com.bhak.project.controller;

import com.bhak.project.dto.RegisterRequest;
import com.bhak.project.entity.User;
import com.bhak.project.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// controller pour le CRUD admin des utilisateurs
// tous les endpoints necessitent le role ADMIN
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Administration des comptes", description = "CRUD complet réservé aux administrateurs")
public class UserController {

    private final UserService userService;

    // liste paginee des comptes
    // GET /api/admin/users?page=0&size=10&sort=username
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les comptes (paginé)")
    @ApiResponse(responseCode = "200", description = "Page de comptes retournée")
    public ResponseEntity<Page<User>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        log.info("GET /api/admin/users  page={} size={} sort={}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    // detail d'un compte par son id
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consulter un compte par ID")
    @ApiResponse(responseCode = "200", description = "Compte trouvé")
    @ApiResponse(responseCode = "404", description = "Compte inexistant")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        log.info("GET /api/admin/users/{}", id);
        return ResponseEntity.ok(userService.findById(id));
    }

    // creation d'un compte
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un compte utilisateur")
    @ApiResponse(responseCode = "201", description = "Compte créé")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @ApiResponse(responseCode = "409", description = "Pseudo / mail / tel déjà pris")
    public ResponseEntity<User> create(@RequestBody RegisterRequest request) {
        log.info("POST /api/admin/users  pseudo={}", request.getUsername());
        User created = userService.create(
                request.getUsername(), request.getEmail(),
                request.getPhoneNumber(), request.getPassword(),
                request.getRoleType());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // modification d'un compte
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un compte existant")
    @ApiResponse(responseCode = "200", description = "Compte mis à jour")
    @ApiResponse(responseCode = "404", description = "Compte inexistant")
    @ApiResponse(responseCode = "409", description = "Conflit d'unicité")
    public ResponseEntity<User> update(@PathVariable Long id,
                                       @RequestBody RegisterRequest request) {
        log.info("PUT /api/admin/users/{}", id);
        User updated = userService.update(id,
                request.getUsername(), request.getEmail(),
                request.getPhoneNumber(), request.getPassword(),
                request.getRoleType());
        return ResponseEntity.ok(updated);
    }

    // suppression d'un compte
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un compte")
    @ApiResponse(responseCode = "204", description = "Compte supprimé")
    @ApiResponse(responseCode = "404", description = "Compte inexistant")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/admin/users/{}", id);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
