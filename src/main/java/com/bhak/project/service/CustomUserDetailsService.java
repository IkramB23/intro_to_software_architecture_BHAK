package com.bhak.project.service;

import com.bhak.project.entity.User;
import com.bhak.project.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of {@link org.springframework.security.core.userdetails.UserDetailsService}
 * for Spring Security authentication.
 *
 * <h2>Purpose</h2>
 * Bridges the application's data model ({@link com.bhak.project.entity.User})
 * and the contract expected by Spring Security ({@link org.springframework.security.core.userdetails.UserDetails}).
 * It is used by the {@code DaoAuthenticationProvider} declared in
 * {@link com.bhak.project.configuration.SecurityConfig} to load a user during
 * credential verification (login) or JWT validation.
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>Looks up the account by username via {@code UserRepository#findByUsername()}.</li>
 *   <li>Checks that the account exists and has {@link com.bhak.project.entity.Credentials}.</li>
 *   <li>Builds a Spring Security {@code User} object with the username, hashed password,
 *       and an authority {@code ROLE_<ROLE_NAME>} (e.g. {@code ROLE_ADMIN}).
 *       This {@code ROLE_} prefix is required by Spring Security for
 *       {@code hasRole("ADMIN")} to work correctly.</li>
 * </ol>
 *
 * <h2>Technologies</h2>
 * Spring Security ({@code UserDetailsService}, {@code SimpleGrantedAuthority}),
 * Lombok ({@code @RequiredArgsConstructor}, {@code @Slf4j}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User account = userRepository.findByUsername(username);
        if (account == null) {
            log.warn("Compte inconnu : {}", username);
            throw new UsernameNotFoundException("Aucun compte associé au pseudo : " + username);
        }
        if (account.getCredentials() == null) {
            log.error("Credentials absents pour le compte : {}", username);
            throw new UsernameNotFoundException("Données d'authentification absentes pour : " + username);
        }

        String authority = "ROLE_" + account.getRole().getName().name();
        log.debug("Compte chargé : {} | autorité : {}", username, authority);

        return new org.springframework.security.core.userdetails.User(
                account.getUsername(),
                account.getCredentials().getPassword(),
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}
