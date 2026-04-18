package com.example.droponairdemo.service;

import com.example.droponairdemo.config.DemoProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Issues and validates the demo's own user JWTs.
 *
 * This is NOT the DropOnAir server secret, it is a completely separate
 * signing key used only for the demo application's own user authentication.
 *
 * In a real application you already have your own auth system (OAuth2, session
 * cookies, etc.). The token exchange flow works with any JWT, DropOnAir
 * only cares about the payload it mints, not your internal format.
 */
@Service
public class DemoJwtService {

    private final SecretKey signingKey;
    private final int expiryHours;

    public DemoJwtService(DemoProperties props) {
        byte[] keyBytes = props.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        // Pad or truncate to 32 bytes for HS256
        byte[] paddedKey = new byte[32];
        System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
        this.signingKey = Keys.hmacShaKeyFor(paddedKey);
        this.expiryHours = props.getJwtExpiryHours();
    }

    /**
     * Issues a demo JWT for the given userId.
     * In production this is already handled by your existing auth system.
     */
    public String issueToken(String userId, String displayName) {
        long nowMs = System.currentTimeMillis();
        Map<String, Object> claims = new HashMap<>();
        claims.put("displayName", displayName);

        return Jwts.builder()
            .subject(userId)
            .issuedAt(new Date(nowMs))
            .expiration(new Date(nowMs + expiryHours * 3600_000L))
            .claims(claims)
            .signWith(signingKey)
            .compact();
    }

    /**
     * Extracts the userId (subject) from a demo JWT. Returns null if invalid.
     */
    public String extractUserId(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.getSubject();
    }

    /**
     * Extracts the display name claim from a demo JWT.
     */
    public String extractDisplayName(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.get("displayName", String.class);
    }
}
