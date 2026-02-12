package com.bhak.project.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

// utilitaire pour generer et valider les tokens jwt
// utilise une cle secrete HMAC-SHA definie dans application.properties
@Component
public class JwtUtils {

    @Value("${app.jwt.secret-key}")
    private String secretKey;

    @Value("${app.jwt.expiration-time}")
    private long expirationMs;

    // ---- generation ----

    // genere un token a partir d'un UserDetails
    public String generateToken(UserDetails userDetails) {
        return buildToken(Map.of(), userDetails.getUsername());
    }

    // genere un token a partir d'un pseudo
    public String generateToken(String username) {
        return buildToken(Map.of(), username);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(signingKey())
                .compact();
    }

    // ---- extraction ----

    public String extractLogin(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // alias pour compatibilite avec le filtre
    public String extractUsername(String token) {
        return extractLogin(token);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseAllClaims(token));
    }

    // ---- validation ----

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractLogin(token).equals(userDetails.getUsername())
                && !isExpired(token);
    }

    // alias
    public boolean validateToken(String token, UserDetails userDetails) {
        return isTokenValid(token, userDetails);
    }

    // ---- utilitaires prives ----

    private boolean isExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
