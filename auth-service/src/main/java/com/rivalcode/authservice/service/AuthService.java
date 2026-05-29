package com.rivalcode.authservice.service;

import com.rivalcode.authservice.jwt.JwtTokenProvider;
import com.rivalcode.authservice.jwt.JwtTokenProvider;
import com.rivalcode.authservice.model.*;
import com.rivalcode.authservice.repository.*;
import com.rivalcode.contracts.users.model.*;
import com.rivalcode.contracts.users.enums.UserRole;
import com.rivalcode.authservice.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthTokensResponse register(RegisterRequest request) {
        if (userRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            throw new DuplicateException("Email already exists");
        }
        if (userRepository.findByUsernameIgnoreCase(request.getUsername()).isPresent()) {
            throw new DuplicateException("Username already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .displayName(request.getDisplayName())
                .enabled(true)
                .build();
        user = userRepository.save(user);

        Credential credential = Credential.builder()
                .user(user)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .passwordAlgo("bcrypt")
                .passwordUpdatedAt(Instant.now())
                .build();
        credentialRepository.save(credential);

        UserRoleEntity role = UserRoleEntity.builder()
                .id(new UserRoleEntity.UserRoleId(user.getUserId(), UserRole.USER.name()))
                .user(user)
                .build();
        userRoleRepository.save(role);

        return buildTokensResponse(user, List.of(UserRole.USER));
    }

    public AuthTokensResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(request.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsernameIgnoreCase(request.getUsernameOrEmail());
        }
        User user = userOpt.orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        Credential cred = credentialRepository.findById(user.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), cred.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.getEnabled()) {
            throw new UnauthorizedException("Account disabled");
        }

        List<UserRole> roles = userRoleRepository.findByUser_UserId(user.getUserId())
                .stream()
                .map(r -> UserRole.valueOf(r.getId().getRole()))
                .collect(Collectors.toList());

        return buildTokensResponse(user, roles);
    }

    public AuthTokensResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        UUID userId = jwtTokenProvider.getUserIdFromRefreshToken(request.getRefreshToken());
        String hash = hashToken(request.getRefreshToken());
        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (tokenEntity.getRevokedAt() != null) {
            log.warn("Attempt to use revoked refresh token for user {}", tokenEntity.getUserId());
            throw new UnauthorizedException("Refresh token revoked");
        }
        if (tokenEntity.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Attempt to use expired refresh token for user {}", tokenEntity.getUserId());
            throw new UnauthorizedException("Refresh token expired");
        }

        User user = userRepository.findById(tokenEntity.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        tokenEntity.setRevokedAt(Instant.now());
        refreshTokenRepository.save(tokenEntity);

        List<UserRole> roles = userRoleRepository.findByUser_UserId(user.getUserId())
                .stream()
                .map(r -> UserRole.valueOf(r.getId().getRole()))
                .collect(Collectors.toList());

        return buildTokensResponse(user, roles);
    }

    private AuthTokensResponse buildTokensResponse(User user, List<UserRole> roles) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getUsername(), roles);
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getUserId());

        String refreshHash = hashToken(refreshTokenStr);
        RefreshTokenEntity refreshEntity = RefreshTokenEntity.builder()
                .userId(user.getUserId())
                .tokenHash(refreshHash)
                .expiresAt(Instant.now().plusSeconds(2592000))
                .build();
        refreshTokenRepository.save(refreshEntity);

        UserProfileDto profile = UserProfileDto.builder()
                .userId(user.getUserId().toString())
                .email(user.getEmail())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .roles(roles)
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return AuthTokensResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresInSeconds(900L)
                .user(profile)
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}