package com.bhak.project.service;

import com.bhak.project.entity.*;
import com.bhak.project.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService service;

    private User buildUser(RoleType roleType) {
        Role role = new Role(1L, roleType, roleType.getLabel());
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(role);

        Credentials cred = new Credentials();
        cred.setEmail("alice@test.com");
        cred.setPassword("$2a$hashed");
        cred.setUser(user);
        user.setCredentials(cred);
        return user;
    }

    @Test
    void loadByUsername_existingUser_returnsUserDetails() {
        when(userRepository.findByUsername("alice")).thenReturn(buildUser(RoleType.USER));

        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("$2a$hashed");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadByUsername_adminRole_hasAdminAuthority() {
        when(userRepository.findByUsername("admin")).thenReturn(buildUser(RoleType.ADMIN));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadByUsername_unknownUser_throws() {
        when(userRepository.findByUsername("unknown")).thenReturn(null);

        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadByUsername_nullCredentials_throws() {
        User user = new User();
        user.setId(1L);
        user.setUsername("nocred");
        user.setRole(new Role(1L, RoleType.USER, "Utilisateur"));
        user.setCredentials(null);

        when(userRepository.findByUsername("nocred")).thenReturn(user);

        assertThatThrownBy(() -> service.loadUserByUsername("nocred"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
