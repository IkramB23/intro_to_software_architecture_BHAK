package com.bhak.project.configuration;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    // cle de test en Base64 (256 bits minimum pour HMAC-SHA)
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "expirationMs", 3600000L); // 1h
    }

    @Test
    void generateToken_fromUsername_returnsNonBlank() {
        String token = jwtUtils.generateToken("alice");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
    }

    @Test
    void extractLogin_returnsSubject() {
        String token = jwtUtils.generateToken("alice");

        assertThat(jwtUtils.extractLogin(token)).isEqualTo("alice");
    }

    @Test
    void extractUsername_alias_works() {
        String token = jwtUtils.generateToken("bob");

        assertThat(jwtUtils.extractUsername(token)).isEqualTo("bob");
    }

    @Test
    void isTokenValid_withMatchingUser_returnsTrue() {
        String token = jwtUtils.generateToken("alice");
        UserDetails userDetails = new User("alice", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(jwtUtils.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_withDifferentUser_returnsFalse() {
        String token = jwtUtils.generateToken("alice");
        UserDetails userDetails = new User("bob", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(jwtUtils.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_throwsOrReturnsFalse() {
        // set TTL a 0 ms pour generer un token deja expire
        ReflectionTestUtils.setField(jwtUtils, "expirationMs", 0L);
        String token = jwtUtils.generateToken("alice");

        UserDetails userDetails = new User("alice", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // JJWT lance ExpiredJwtException au parsing d'un token expire
        assertThatThrownBy(() -> jwtUtils.isTokenValid(token, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void validateToken_alias_works() {
        String token = jwtUtils.generateToken("alice");
        UserDetails userDetails = new User("alice", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(jwtUtils.validateToken(token, userDetails)).isTrue();
    }

    @Test
    void extractExpiration_returnsDateInFuture() {
        String token = jwtUtils.generateToken("alice");

        assertThat(jwtUtils.extractExpiration(token)).isInTheFuture();
    }

    @Test
    void generateToken_fromUserDetails_works() {
        UserDetails userDetails = new User("alice", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtUtils.generateToken(userDetails);

        assertThat(jwtUtils.extractLogin(token)).isEqualTo("alice");
    }
}
