package com.campustrade.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank String username,
            @Size(min = 8, max = 64) String password,
            @NotBlank String nickname) {
    }

    public record LoginRequest(@NotBlank String account, @NotBlank String password) {
    }

    public record LoginResponse(Long userId, String nickname, String role, String accessToken) {
    }

    public record UserSummary(Long id, String username, String nickname, String role) {
    }
}
