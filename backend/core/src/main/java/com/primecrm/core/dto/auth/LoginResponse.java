package com.primecrm.core.dto.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        MeResponse user
) {
    public static LoginResponse of(String accessToken, String refreshToken, long expiresInSeconds, MeResponse user) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
