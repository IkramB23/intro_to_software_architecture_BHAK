package com.bhak.project.service;

import com.bhak.project.entity.Credentials;
import com.bhak.project.entity.Role;
import com.bhak.project.entity.RoleType;
import com.bhak.project.entity.User;
import com.bhak.project.exception.DuplicateResourceException;
import com.bhak.project.exception.InvalidRequestException;
import com.bhak.project.exception.ResourceNotFoundException;
import com.bhak.project.repository.CredentialsRepository;
import com.bhak.project.repository.RoleRepository;
import com.bhak.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business service managing the lifecycle of user accounts.
 *
 * <h2>Purpose</h2>
 * Encapsulates all user-related business logic:
 * <ul>
 *   <li><b>Read</b>: paginated account listing ({@code findAll}) and individual
 *       lookup ({@code findById}).</li>
 *   <li><b>Create</b>: uniqueness checks (username, email, phone),
 *       role resolution, BCrypt password hashing, then persistence
 *       of the {@code User + Credentials} aggregate.</li>
 *   <li><b>Update</b>: partial update (only non-null fields are modified)
 *       with re-validation of uniqueness.</li>
 *   <li><b>Delete</b>: removes the {@code User}; the associated {@code Credentials}
 *       are automatically deleted thanks to {@code CascadeType.ALL}
 *       and {@code orphanRemoval=true}.</li>
 * </ul>
 *
 * <h2>How it works</h2>
 * Each public method is annotated with {@code @Transactional} (or {@code @Transactional(readOnly=true)}
 * for reads). Custom exceptions ({@code ResourceNotFoundException},
 * {@code DuplicateResourceException}, {@code InvalidRequestException}) raised
 * here are intercepted by {@link com.bhak.project.exception.GlobalExceptionHandler}.
 *
 * <h2>Technologies</h2>
 * Spring Data JPA, Spring Security ({@code PasswordEncoder}), {@code @Transactional},
 * Lombok ({@code @RequiredArgsConstructor}, {@code @Slf4j}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredentialsRepository credentialsRepository;
    private final PasswordEncoder passwordEncoder;

    // ---- lecture ----

    // retourne une page d'utilisateurs
    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        log.info("Chargement page {} (taille {})", pageable.getPageNumber(), pageable.getPageSize());
        return userRepository.findAll(pageable);
    }

    // recherche un user par son id
    @Transactional(readOnly = true)
    public User findById(Long id) {
        log.info("Recherche du compte #{}", id);
        return fetchUserOrThrow(id);
    }

    // ---- creation ----

    // cree un nouveau compte apres verification de l'unicite
    @Transactional
    public User create(String username, String email, String phoneNumber,
                       String password, RoleType roleType) {
        log.info("Création du compte [{}]", username);

        ensureUsernameAvailable(username, null);
        ensureEmailAvailable(email, null);
        if (phoneNumber != null && !phoneNumber.isBlank()) {
            ensurePhoneAvailable(phoneNumber, null);
        }

        Role role = fetchRoleOrThrow(roleType);

        User user = new User();
        user.setUsername(username);
        user.setRole(role);

        Credentials cred = new Credentials();
        cred.setEmail(email);
        cred.setPhoneNumber(phoneNumber);
        cred.setPassword(passwordEncoder.encode(password));
        cred.setUser(user);
        user.setCredentials(cred);

        User saved = userRepository.save(user);
        log.info("Compte [{}] créé avec l'ID #{}", saved.getUsername(), saved.getId());
        return saved;
    }

    // ---- modification ----

    // mise a jour partielle : seuls les champs non-null sont modifies
    @Transactional
    public User update(Long id, String username, String email,
                       String phoneNumber, String password, RoleType roleType) {
        log.info("Mise à jour du compte #{}", id);
        User existing = fetchUserOrThrow(id);

        if (username != null) {
            ensureUsernameAvailable(username, id);
            existing.setUsername(username);
        }

        if (roleType != null) {
            existing.setRole(fetchRoleOrThrow(roleType));
        }

        Credentials cred = existing.getCredentials();
        if (email != null) {
            ensureEmailAvailable(email, id);
            cred.setEmail(email);
        }
        if (phoneNumber != null) {
            ensurePhoneAvailable(phoneNumber, id);
            cred.setPhoneNumber(phoneNumber);
        }
        if (password != null && !password.isBlank()) {
            cred.setPassword(passwordEncoder.encode(password));
        }

        User saved = userRepository.save(existing);
        log.info("Compte #{} mis à jour", id);
        return saved;
    }

    // ---- suppression ----

    // supprime un user et ses credentials (grace au cascade)
    @Transactional
    public void delete(Long id) {
        log.info("Suppression du compte #{}", id);
        User user = fetchUserOrThrow(id);
        userRepository.delete(user);
        log.info("Compte #{} supprimé", id);
    }

    // ---- methodes utilitaires ----

    private User fetchUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compte", id));
    }

    private Role fetchRoleOrThrow(com.bhak.project.entity.RoleType type) {
        Role role = roleRepository.findByName(type);
        if (role == null) {
            throw new InvalidRequestException("Le niveau d'autorisation '" + type + "' n'est pas référencé");
        }
        return role;
    }

    // verifie que le pseudo n'est pas deja pris par un autre compte
    private void ensureUsernameAvailable(String username, Long excludeId) {
        User found = userRepository.findByUsername(username);
        if (found != null && !found.getId().equals(excludeId)) {
            throw new DuplicateResourceException("pseudo", username);
        }
    }

    private void ensureEmailAvailable(String email, Long excludeUserId) {
        Credentials found = credentialsRepository.findByEmail(email);
        if (found != null && !found.getUser().getId().equals(excludeUserId)) {
            throw new DuplicateResourceException("mail", email);
        }
    }

    private void ensurePhoneAvailable(String phone, Long excludeUserId) {
        Credentials found = credentialsRepository.findByPhoneNumber(phone);
        if (found != null && !found.getUser().getId().equals(excludeUserId)) {
            throw new DuplicateResourceException("tel", phone);
        }
    }
}
