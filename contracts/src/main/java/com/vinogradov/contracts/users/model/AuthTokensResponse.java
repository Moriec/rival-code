package com.vinogradov.contracts.users.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthTokensResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresInSeconds;
    private UserProfileDto user;
}
