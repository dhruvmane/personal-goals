package com.personalgoals.auth;

import com.personalgoals.user.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        UserResponse user
) {
    public static AuthResponse bearer(
            String token,
            long expiresIn,
            String refreshToken,
            long refreshExpiresIn,
            UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresIn, refreshToken, refreshExpiresIn, user);
    }
}
