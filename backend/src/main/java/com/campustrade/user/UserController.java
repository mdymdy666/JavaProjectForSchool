package com.campustrade.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campustrade.auth.AuthService;
import com.campustrade.auth.UserAccount;
import com.campustrade.common.ApiResponse;
import com.campustrade.security.SecurityUser;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    ApiResponse<UserView> me(@AuthenticationPrincipal SecurityUser principal) {
        UserAccount user = authService.requireUser(principal.userId());
        return ApiResponse.success(new UserView(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getRole()));
    }

    public record UserView(
            Long id,
            String username,
            String nickname,
            String phone,
            String email,
            String avatarUrl,
            String role) {
    }
}
