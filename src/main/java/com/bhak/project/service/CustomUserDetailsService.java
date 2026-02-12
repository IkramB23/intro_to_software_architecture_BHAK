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

// charge les infos d'un utilisateur pour spring security
// transforme notre entite User en UserDetails
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
