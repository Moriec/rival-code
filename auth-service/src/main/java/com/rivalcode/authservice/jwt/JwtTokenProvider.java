package com.rivalcode.authservice.jwt;

import com.rivalcode.contracts.users.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    @Value("${jwt.access-token-expiration-seconds}")
    private long accessExpiration;

    @Value("${jwt.refresh-token-expiration-seconds}")
    private long refreshExpiration;

    @Value("${jwt.issuer}")
    private String issuer;

    private static final String ACCESS_SECRET_STRING = "dGhpc2lzYXNlY3JldGZvcmFjY2Vzc3Rva2VuMTIzNDU2Nzg5MGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6";
    private static final String REFRESH_SECRET_STRING = "cmVmcmVzaHRva2Vuc2VjcmV0a2V5Zm9ycmVmcmVzaHRva2VuczEyMzQ1Njc4OTBhYmNkZWZnaGlqa2xtbm9wcXJzdHV2d3h5eg==";

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;

    public JwtTokenProvider() {
        byte[] accessBytes = Base64.getDecoder().decode(ACCESS_SECRET_STRING);
        byte[] refreshBytes = Base64.getDecoder().decode(REFRESH_SECRET_STRING);
        this.accessSecretKey = Keys.hmacShaKeyFor(accessBytes);
        this.refreshSecretKey = Keys.hmacShaKeyFor(refreshBytes);
    }

    public String generateAccessToken(UUID userId, String username, List<UserRole> roles) {
        Instant now = Instant.now();
        String rolesString = roles.stream().map(Enum::name).collect(Collectors.joining(" "));

        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpiration)))
                .claim("username", username)
                .claim("scope", rolesString)
                .signWith(accessSecretKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshExpiration)))
                .claim("type", "refresh")
                .signWith(refreshSecretKey)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(accessSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Claims parseRefreshToken(String token) {
        return Jwts.parser()
                .verifyWith(refreshSecretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateAccessToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            parseRefreshToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public UUID getUserIdFromAccessToken(String token) {
        Claims claims = parseAccessToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public UUID getUserIdFromRefreshToken(String token) {
        Claims claims = parseRefreshToken(token);
        return UUID.fromString(claims.getSubject());
    }
}