package com.campustrade.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campustrade.common.BusinessException;
import com.campustrade.common.ErrorCode;
import com.campustrade.security.JwtService;
import com.campustrade.cache.RedisSupport;
import com.campustrade.cache.CacheNames;
import java.time.Duration;

import static com.campustrade.auth.AuthDtos.*;

@Service
public class AuthService {
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisSupport redisSupport;

    public AuthService(AuthMapper authMapper, PasswordEncoder passwordEncoder, JwtService jwtService,
            RedisSupport redisSupport) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.redisSupport = redisSupport;
    }

    public CaptchaResponse registerCaptcha(String account) {
        String code = "123456";
        redisSupport.put(CacheNames.REGISTER_CAPTCHA + account, code, Duration.ofMinutes(5));
        return new CaptchaResponse(code, 300);
    }

    @Transactional
    public UserSummary register(RegisterRequest request) {
        if (request.captcha() != null && !request.captcha().isEmpty()) {
            String cached = redisSupport.get(CacheNames.REGISTER_CAPTCHA + request.username()).orElse(null);
            if (cached == null || !cached.equals(request.captcha())) {
                throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
            }
            redisSupport.delete(CacheNames.REGISTER_CAPTCHA + request.username());
        }
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

    public LoginResponse login(LoginRequest request, String clientIp) {
        String rateKey = CacheNames.LOGIN_RATE + clientIp;
        long attempts = redisSupport.incrementBy(rateKey, 1, Duration.ofMinutes(10)).orElse(0);
        if (attempts > 5) {
            throw new BusinessException(ErrorCode.RATE_LIMITED);
        }
        UserAccount user = authMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .and(query -> query.eq(UserAccount::getUsername, request.account())
                        .or().eq(UserAccount::getPhone, request.account())
                        .or().eq(UserAccount::getEmail, request.account()))
                .last("LIMIT 1"));
        if (user == null || !"NORMAL".equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "账号或密码错误");
        }
        redisSupport.delete(rateKey);
        return new LoginResponse(
                user.getId(),
                user.getNickname(),
                user.getRole(),
                jwtService.create(user.getId(), user.getRole()));
    }

    public void logout(String token) {
        String id = jwtService.extractId(token);
        redisSupport.put(CacheNames.JWT_BLACKLIST + id, "1",
                Duration.ofHours(8));
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
