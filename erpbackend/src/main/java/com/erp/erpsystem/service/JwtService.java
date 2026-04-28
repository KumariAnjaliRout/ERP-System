package com.erp.erpsystem.service;

import com.erp.erpsystem.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secret:#{null}}")
    private String secret;

    // Default: 8600000ms (~2.4 hours). Override via jwt.expiration-ms in application.yml
    @Value("${jwt.expiration-ms:8600000}")
    private long expirationMs;

    @Value("${jwt.issuer:erp-system}")
    private String issuer;

    private Key key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set jwt.secret in application.yml");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters for HS256 algorithm");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.debug("JWT Service initialized with issuer: {}, expiration: {}ms", issuer, expirationMs);
    }


    public String generateToken(User user, String outletId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("role", "ROLE_" + user.getRole().name());

        if (user.getOrganizationId() != null)
            claims.put("organizationId", user.getOrganizationId());

        if (outletId != null)
            claims.put("outletId", outletId);

        return Jwts.builder()
                .setClaims(claims)
                // FIX: setSubject added — JWT standard requires a subject
                .setSubject(user.getId().toString())
                .setIssuer(issuer)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    public boolean validateToken(String token) {
        // FIX: Added null/blank check before parsing
        if (token == null || token.isBlank()) {
            log.warn("JWT token is null or blank");
            return false;
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token format: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT signature validation failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
        }
        return false;
    }


     public Claims extractAllClaims(String token) throws JwtException {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be null or blank");
        }
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    // FIX: Extract subject (userId) from standard JWT subject field
    public String extractSubject(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            log.warn("Failed to extract subject from token: {}", e.getMessage());
            return null;
        }
    }


    public String extractRole(String token) {
        try {
            String role = extractClaim(token, claims -> claims.get("role", String.class));
            if (role != null && !role.startsWith("ROLE_")) {
                role = "ROLE_" + role;
                log.warn("Role in JWT missing ROLE_ prefix, fixed to: {}", role);
            }
            return role;
        } catch (Exception e) {
            log.warn("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }


    public String extractRoleWithoutPrefix(String token) {
        try {
            String roleWithPrefix = extractRole(token);
            if (roleWithPrefix != null && roleWithPrefix.startsWith("ROLE_")) {
                return roleWithPrefix.substring(5);
            }
            return roleWithPrefix;
        } catch (Exception e) {
            log.warn("Failed to extract role without prefix: {}", e.getMessage());
            return null;
        }
    }


    public String extractUserId(String token) {
        try {
            // FIX: Extract from both custom claim AND subject for reliability
            String userId = extractClaim(token, claims -> claims.get("userId", String.class));
            if (userId == null) {
                // Fallback to subject if custom claim is missing
                userId = extractSubject(token);
                if (userId != null) {
                    log.warn("userId claim missing, fell back to JWT subject");
                }
            }
            return userId;
        } catch (Exception e) {
            log.warn("Failed to extract userId from token: {}", e.getMessage());
            return null;
        }
    }


    public String extractOrganizationId(String token) {
        try {
            return extractClaim(token, claims -> claims.get("organizationId", String.class));
        } catch (Exception e) {
            log.warn("Failed to extract organizationId from token: {}", e.getMessage());
            return null;
        }
    }


    public String extractOutletId(String token) {
        try {
            return extractClaim(token, claims -> claims.get("outletId", String.class));
        } catch (Exception e) {
            log.warn("Failed to extract outletId from token: {}", e.getMessage());
            return null;
        }
    }


    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            log.warn("Failed to extract expiration from token: {}", e.getMessage());
            return null;
        }
    }


    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.warn("Failed to check token expiration: {}", e.getMessage());
            return true;
        }
    }


    // FIX: isTokenExpired check now happens BEFORE extractUserId to avoid double parse
    public boolean validateTokenForUser(String token, String userId) {
        try {
            if (isTokenExpired(token)) return false;
            String tokenUserId = extractUserId(token);
            return tokenUserId != null && tokenUserId.equals(userId);
        } catch (Exception e) {
            log.warn("Token validation failed for user {}: {}", userId, e.getMessage());
            return false;
        }
    }


    // FIX: Returns 0 (not negative) when token is expired, callers handle it cleanly
    public long getRemainingTimeInMs(String token) {
        try {
            Date expiration = extractExpiration(token);
            if (expiration == null) return -1L;
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0L);
        } catch (Exception e) {
            log.warn("Failed to get remaining time: {}", e.getMessage());
            return -1L;
        }
    }

    public long getRemainingTimeInHours(String token) {
        return getRemainingTimeInMs(token) / (1000 * 60 * 60);
    }

    public long getRemainingTimeInMinutes(String token) {
        return getRemainingTimeInMs(token) / (1000 * 60);
    }



    public Map<String, Object> getTokenInfo(String token) {
        Map<String, Object> info = new HashMap<>();
        try {
            Claims claims = extractAllClaims(token);
            info.put("subject", claims.getSubject());
            info.put("issuer", claims.getIssuer());
            info.put("issuedAt", claims.getIssuedAt());
            info.put("expiration", claims.getExpiration());
            info.put("userId", claims.get("userId", String.class));
            info.put("role", claims.get("role", String.class));
            info.put("organizationId", claims.get("organizationId", String.class));
            info.put("outletId", claims.get("outletId", String.class));
            info.put("valid", validateToken(token));
            info.put("expired", isTokenExpired(token));
        } catch (ExpiredJwtException e) {
            Claims claims = e.getClaims();
            info.put("userId", claims.get("userId", String.class));
            info.put("role", claims.get("role", String.class));
            info.put("organizationId", claims.get("organizationId", String.class));
            info.put("valid", false);
            info.put("expired", true);
        } catch (Exception e) {
            info.put("error", e.getMessage());
            info.put("valid", false);
        }
        return info;
    }


    public long getRemainingTimeInSeconds(String token) {
        return getRemainingTimeInMs(token) / 1000;
    }

}