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

/**
 * Utility for generating and validating JWT (JSON Web Token) tokens.
 *
 * <h2>Purpose</h2>
 * Centralises all JWT logic in the application:
 * <ul>
 *   <li>Generating a signed token from a username or a {@code UserDetails}.</li>
 *   <li>Extracting claims (subject, expiration date, custom claims).</li>
 *   <li>Validating (signature + expiration) a token against a {@code UserDetails}.</li>
 * </ul>
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>The secret key is read from {@code application.properties} ({@code app.jwt.secret-key})
 *       and Base64-decoded to produce an HMAC-SHA {@link javax.crypto.SecretKey}.</li>
 *   <li>The validity duration ({@code app.jwt.expiration-time}, in milliseconds) is also
 *       injected via {@code @Value}.</li>
 *   <li>The token is built with the <b>JJWT</b> library (io.jsonwebtoken) which handles
 *       HMAC-SHA256/384/512 signing and automatic verification.</li>
 *   <li>The {@code isTokenValid()} and {@code validateToken()} methods check that
 *       the subject matches the {@code UserDetails} <em>and</em> that the token has not expired.</li>
 * </ol>
 *
 * <h2>Technologies</h2>
 * JJWT ({@code io.jsonwebtoken}), Spring {@code @Component}, {@code @Value}.
 */
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
