package com.bhak.project.service;

import com.bhak.project.entity.*;
import com.bhak.project.exception.DuplicateResourceException;
import com.bhak.project.exception.InvalidRequestException;
import com.bhak.project.exception.ResourceNotFoundException;
import com.bhak.project.repository.CredentialsRepository;
import com.bhak.project.repository.RoleRepository;
import com.bhak.project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private CredentialsRepository credentialsRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private Role userRole;
    private User existingUser;
    private Credentials existingCred;

    @BeforeEach
    void setUp() {
        userRole = new Role(1L, RoleType.USER, "Utilisateur");

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("alice");
        existingUser.setRole(userRole);

        existingCred = new Credentials();
        existingCred.setId(1L);
        existingCred.setEmail("alice@test.com");
        existingCred.setPhoneNumber("0600000000");
        existingCred.setPassword("hashed");
        existingCred.setUser(existingUser);
        existingUser.setCredentials(existingCred);
    }

    // ---- findAll ----

    @Test
    void findAll_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(List.of(existingUser));
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
    }

    // ---- findById ----

    @Test
    void findById_existingId_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        User result = userService.findById(1L);

        assertThat(result.getUsername()).isEqualTo("alice");
    }

    @Test
    void findById_unknownId_throws404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- create ----

    @Test
    void create_success() {
        when(userRepository.findByUsername("bob")).thenReturn(null);
        when(credentialsRepository.findByEmail("bob@test.com")).thenReturn(null);
        when(roleRepository.findByName(RoleType.USER)).thenReturn(userRole);
        when(passwordEncoder.encode("pass123")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        User result = userService.create("bob", "bob@test.com", null, "pass123", RoleType.USER);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getUsername()).isEqualTo("bob");
        assertThat(result.getCredentials().getEmail()).isEqualTo("bob@test.com");
        assertThat(result.getCredentials().getPassword()).isEqualTo("$2a$hashed");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_duplicateUsername_throws409() {
        when(userRepository.findByUsername("alice")).thenReturn(existingUser);

        assertThatThrownBy(() -> userService.create("alice", "new@test.com", null, "pass", RoleType.USER))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_duplicateEmail_throws409() {
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(credentialsRepository.findByEmail("alice@test.com")).thenReturn(existingCred);

        assertThatThrownBy(() -> userService.create("newuser", "alice@test.com", null, "pass", RoleType.USER))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void create_duplicatePhone_throws409() {
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(credentialsRepository.findByEmail("new@test.com")).thenReturn(null);
        when(credentialsRepository.findByPhoneNumber("0600000000")).thenReturn(existingCred);

        assertThatThrownBy(() ->
                userService.create("newuser", "new@test.com", "0600000000", "pass", RoleType.USER))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void create_unknownRole_throws400() {
        when(userRepository.findByUsername("bob")).thenReturn(null);
        when(credentialsRepository.findByEmail("bob@test.com")).thenReturn(null);
        when(roleRepository.findByName(RoleType.ADMIN)).thenReturn(null);

        assertThatThrownBy(() -> userService.create("bob", "bob@test.com", null, "pass", RoleType.ADMIN))
                .isInstanceOf(InvalidRequestException.class);
    }

    // ---- update ----

    @Test
    void update_partialFields_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("alice_new")).thenReturn(null);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.update(1L, "alice_new", null, null, null, null);

        assertThat(result.getUsername()).isEqualTo("alice_new");
    }

    @Test
    void update_unknownId_throws404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, "x", null, null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_duplicateUsername_throws409() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("taken");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByUsername("taken")).thenReturn(otherUser);

        assertThatThrownBy(() -> userService.update(1L, "taken", null, null, null, null))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ---- delete ----

    @Test
    void delete_existingId_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.delete(1L);

        verify(userRepository).delete(existingUser);
    }

    @Test
    void delete_unknownId_throws404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
