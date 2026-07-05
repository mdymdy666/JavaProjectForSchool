package com.campustrade.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campustrade.common.BusinessException;
import com.campustrade.common.ErrorCode;
import com.campustrade.security.JwtService;

import static com.campustrade.auth.AuthDtos.*;

@Service
public class AuthService {
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthMapper authMapper, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserSummary register(RegisterRequest request) {
        Long count = authMapper.selectCount(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUsername, request.username()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "用户名已存在");
        }
        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setRole("USER");
        user.setStatus("NORMAL");
        authMapper.insert(user);
        return summary(user);
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount user = authMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .and(query -> query.eq(UserAccount::getUsername, request.account())
                        .or().eq(UserAccount::getPhone, request.account())
                        .or().eq(UserAccount::getEmail, request.account()))
                .last("LIMIT 1"));
        if (user == null || !"NORMAL".equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        return new LoginResponse(
                user.getId(),
                user.getNickname(),
                user.getRole(),
                jwtService.create(user.getId(), user.getRole()));
    }

    public UserAccount requireUser(long userId) {
        UserAccount user = authMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserSummary summary(UserAccount user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getNickname(), user.getRole());
    }
}
