package com.campustrade.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final SecretKey key;

    public JwtService(@Value("${campus-trade.jwt-secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String create(long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofHours(8))))
                .signWith(key)
                .compact();
    }

    public SecurityUser parse(String token) {
        Claims claims = parseClaims(token);
        return new SecurityUser(Long.parseLong(claims.getSubject()), claims.get("role", String.class));
    }

    public String extractId(String token) {
        return parseClaims(token).getId();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
