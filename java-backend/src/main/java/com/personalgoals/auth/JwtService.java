package com.personalgoals.auth;

import com.personalgoals.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    static final String TYPE_ACCESS = "access";
    static final String TYPE_REFRESH = "refresh";

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_ID = "jti";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey key;
    private final Duration expiresIn;
    private final Duration refreshExpiresIn;
    private final String issuer;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expires-in}") Duration expiresIn,
            @Value("${jwt.refresh-expires-in}") Duration refreshExpiresIn,
            @Value("${jwt.issuer}") String issuer) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be at least " + MIN_SECRET_BYTES + " characters. Set JWT_SECRET in your .env file.");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiresIn = expiresIn;
        this.refreshExpiresIn = refreshExpiresIn;
        this.issuer = issuer;
    }

    public String generateAccessToken(User user) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(expiresIn)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(refreshExpiresIn)))
                .signWith(key)
                .compact();
    }

    public UUID parseUserId(String token, String expectedType) throws JwtException {
        Claims claims = parseClaims(token);
        String type = claims.get(CLAIM_TYPE, String.class);
        if (!expectedType.equals(type)) {
            throw new JwtException("Expected a " + expectedType + " token but received " + type);
        }
        return UUID.fromString(claims.getSubject());
    }

    private Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long expiresInSeconds() {
        return expiresIn.toSeconds();
    }

    public long refreshExpiresInSeconds() {
        return refreshExpiresIn.toSeconds();
    }

    public Instant refreshExpiry() {
        return Instant.now().plus(refreshExpiresIn);
    }
}
