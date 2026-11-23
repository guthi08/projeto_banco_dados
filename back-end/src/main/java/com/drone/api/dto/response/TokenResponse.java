package com.drone.api.dto.response;

public record TokenResponse(
        String token,
        String tipo,
        long expiresIn,
        AuthUserResponse usuario
) {
    public static TokenResponse bearer(String token, long expiresInMs, AuthUserResponse usuario) {
        return new TokenResponse(token, "Bearer", expiresInMs / 1000L, usuario);
    }
}
