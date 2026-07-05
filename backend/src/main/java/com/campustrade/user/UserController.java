package com.campustrade.user;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campustrade.auth.AuthMapper;
import com.campustrade.auth.UserAccount;
import com.campustrade.common.ApiResponse;
import com.campustrade.common.BusinessException;
import com.campustrade.common.ErrorCode;
import com.campustrade.security.SecurityUser;
import com.campustrade.product.ProductDtos.ProductCard;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AuthMapper authMapper;
    private final JdbcTemplate jdbc;

    public UserController(AuthMapper authMapper, JdbcTemplate jdbc) {
        this.authMapper = authMapper;
        this.jdbc = jdbc;
    }

    public record ProfileResponse(Long id, String username, String nickname,
            String phone, String email, String avatarUrl, String role) {}

    public record UpdateProfileRequest(
            @Size(max = 50) String nickname,
            String phone,
            String email) {}

    @GetMapping("/me")
    ApiResponse<ProfileResponse> me(Authentication auth) {
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        UserAccount account = authMapper.selectById(user.userId());
        if (account == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        return ApiResponse.success(new ProfileResponse(
                account.getId(), account.getUsername(), account.getNickname(),
                account.getPhone(), account.getEmail(), account.getAvatarUrl(),
                account.getRole()));
    }

    @PutMapping("/me")
    ApiResponse<ProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request,
            Authentication auth) {
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        UserAccount account = authMapper.selectById(user.userId());
        if (account == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        if (request.nickname != null && !request.nickname.isBlank()) {
            account.setNickname(request.nickname.trim());
        }
        if (request.phone != null) account.setPhone(request.phone.trim());
        if (request.email != null) account.setEmail(request.email.trim());
        authMapper.updateById(account);
        return ApiResponse.success(new ProfileResponse(
                account.getId(), account.getUsername(), account.getNickname(),
                account.getPhone(), account.getEmail(), account.getAvatarUrl(),
                account.getRole()));
    }

    @GetMapping("/me/products")
    ApiResponse<List<ProductCard>> myProducts(Authentication auth) {
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.id, p.title, p.price, p.item_condition, p.status, p.view_count,
                       c.name AS category_name, u.nickname AS seller_nickname,
                       (SELECT pi.image_url FROM product_images pi
                        WHERE pi.product_id = p.id ORDER BY pi.sort_order LIMIT 1) AS cover_url,
                       p.created_at
                FROM products p
                JOIN categories c ON c.id = p.category_id
                JOIN users u ON u.id = p.seller_id
                WHERE p.seller_id = ? AND p.deleted = 0
                ORDER BY p.created_at DESC
                """, user.userId());
        List<ProductCard> cards = rows.stream().map(row -> new ProductCard(
                ((Number) row.get("id")).longValue(),
                (String) row.get("title"),
                (java.math.BigDecimal) row.get("price"),
                (String) row.get("item_condition"),
                (String) row.get("status"),
                ((Number) row.get("view_count")).intValue(),
                (String) row.get("category_name"),
                (String) row.get("seller_nickname"),
                (String) row.get("cover_url"),
                row.get("created_at") instanceof java.time.LocalDateTime dt ? dt
                        : ((java.sql.Timestamp) row.get("created_at")).toLocalDateTime()
        )).toList();
        return ApiResponse.success(cards);
    }
}
