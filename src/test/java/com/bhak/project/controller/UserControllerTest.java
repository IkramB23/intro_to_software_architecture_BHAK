package com.bhak.project.controller;

import com.bhak.project.configuration.JwtUtils;
import com.bhak.project.entity.*;
import com.bhak.project.exception.ResourceNotFoundException;
import com.bhak.project.filter.JwtFilter;
import com.bhak.project.service.CustomUserDetailsService;
import com.bhak.project.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private JwtUtils jwtUtils;
    @MockBean private JwtFilter jwtFilter;

    private User sampleUser() {
        Role role = new Role(1L, RoleType.USER, "Utilisateur");
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(role);

        Credentials cred = new Credentials();
        cred.setId(1L);
        cred.setEmail("alice@test.com");
        cred.setUser(user);
        user.setCredentials(cred);
        return user;
    }

    // ---- list ----

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_returns200() throws Exception {
        when(userService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleUser())));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("alice"));
    }

    // ---- getById ----

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_found_returns200() throws Exception {
        when(userService.findById(1L)).thenReturn(sampleUser());

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_notFound_returns404() throws Exception {
        when(userService.findById(99L)).thenThrow(new ResourceNotFoundException("Compte", 99L));

        mockMvc.perform(get("/api/admin/users/99"))
                .andExpect(status().isNotFound());
    }

    // ---- create ----

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_returns201() throws Exception {
        when(userService.create(anyString(), anyString(), any(), anyString(), any()))
                .thenReturn(sampleUser());

        String body = objectMapper.writeValueAsString(Map.of(
                "username", "alice",
                "email", "alice@test.com",
                "password", "pass123"));

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"));
    }

    // ---- update ----

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_returns200() throws Exception {
        User updated = sampleUser();
        updated.setUsername("alice_new");
        when(userService.update(eq(1L), anyString(), any(), any(), any(), any())).thenReturn(updated);

        String body = objectMapper.writeValueAsString(Map.of("username", "alice_new"));

        mockMvc.perform(put("/api/admin/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice_new"));
    }

    // ---- delete ----

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_returns204() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Compte", 99L)).when(userService).delete(99L);

        mockMvc.perform(delete("/api/admin/users/99"))
                .andExpect(status().isNotFound());
    }
}
