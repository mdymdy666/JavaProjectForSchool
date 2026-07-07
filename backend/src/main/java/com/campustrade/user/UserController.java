package com.campustrade.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import jakarta.validation.constraints.NotBlank;
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
            String phone, String email, String avatarUrl, String role,
            Integer creditScore, String realName, String idCardNo, String realNameStatus) {}

    public record UpdateProfileRequest(
            @Size(max = 50) String nickname,
            String phone,
            String email,
            String avatarUrl) {}

    public record AddressResponse(Long id, String receiverName, String receiverPhone,
            String detailAddress, boolean defaultAddress, LocalDateTime createdAt) {}

    public record AddressRequest(
            @NotBlank String receiverName,
            @NotBlank String receiverPhone,
            @NotBlank String detailAddress,
            Boolean defaultAddress) {}

    public record VerificationRequest(@NotBlank String realName, @NotBlank String idCardNo) {}

    @GetMapping("/me")
    ApiResponse<ProfileResponse> me(Authentication auth) {
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        UserAccount account = authMapper.selectById(user.userId());
        if (account == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        return ApiResponse.success(toProfile(account));
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
        if (request.avatarUrl != null) account.setAvatarUrl(request.avatarUrl.trim());
        authMapper.updateById(account);
        return ApiResponse.success(toProfile(account));
    }

    @GetMapping("/me/addresses")
    ApiResponse<List<AddressResponse>> addresses(Authentication auth) {
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        return ApiResponse.success(findAddresses(user.userId()));
    }

    @PostMapping("/me/addresses")
    @Transactional
    ApiResponse<List<AddressResponse>> createAddress(@Valid @RequestBody AddressRequest request,
            Authentication auth) {
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        if (Boolean.TRUE.equals(request.defaultAddress())) {
            clearDefaultAddress(user.userId());
        }
        jdbc.update("""
                INSERT INTO user_addresses(user_id, receiver_name, receiver_phone, detail_address, is_default)
                VALUES (?, ?, ?, ?, ?)
                """, user.userId(), request.receiverName().trim(), request.receiverPhone().trim(),
                request.detailAddress().trim(), Boolean.TRUE.equals(request.defaultAddress()) ? 1 : 0);
        return ApiResponse.success(findAddresses(user.userId()));
    }

    @PutMapping("/me/addresses/{id}")
    @Transactional
    ApiResponse<List<AddressResponse>> updateAddress(@PathVariable Long id,
            @Valid @RequestBody AddressRequest request, Authentication auth) {
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        ensureAddressOwner(id, user.userId());
        if (Boolean.TRUE.equals(request.defaultAddress())) {
            clearDefaultAddress(user.userId());
        }
        jdbc.update("""
                UPDATE user_addresses
                SET receiver_name = ?, receiver_phone = ?, detail_address = ?, is_default = ?
                WHERE id = ? AND user_id = ?
                """, request.receiverName().trim(), request.receiverPhone().trim(),
                request.detailAddress().trim(), Boolean.TRUE.equals(request.defaultAddress()) ? 1 : 0,
                id, user.userId());
        return ApiResponse.success(findAddresses(user.userId()));
    }

    @DeleteMapping("/me/addresses/{id}")
    @Transactional
    ApiResponse<List<AddressResponse>> deleteAddress(@PathVariable Long id, Authentication auth) {
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        ensureAddressOwner(id, user.userId());
        jdbc.update("DELETE FROM user_addresses WHERE id = ? AND user_id = ?", id, user.userId());
        return ApiResponse.success(findAddresses(user.userId()));
    }

    @PostMapping("/me/addresses/{id}/default")
    @Transactional
    ApiResponse<List<AddressResponse>> setDefaultAddress(@PathVariable Long id, Authentication auth) {
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        ensureAddressOwner(id, user.userId());
        clearDefaultAddress(user.userId());
        jdbc.update("UPDATE user_addresses SET is_default = 1 WHERE id = ? AND user_id = ?", id, user.userId());
        return ApiResponse.success(findAddresses(user.userId()));
    }

    @PostMapping("/me/verification")
    @Transactional
    ApiResponse<ProfileResponse> verifyIdentity(@Valid @RequestBody VerificationRequest request,
            Authentication auth) {
        SecurityUser user = (SecurityUser) auth.getPrincipal();
        UserAccount account = authMapper.selectById(user.userId());
        if (account == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        account.setRealName(request.realName().trim());
        account.setIdCardNo(request.idCardNo().trim());
        account.setRealNameStatus("VERIFIED");
        account.setCreditScore(Math.max(account.getCreditScore() == null ? 95 : account.getCreditScore(), 98));
        authMapper.updateById(account);
        return ApiResponse.success(toProfile(account));
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

    private ProfileResponse toProfile(UserAccount account) {
        return new ProfileResponse(
                account.getId(), account.getUsername(), account.getNickname(),
                account.getPhone(), account.getEmail(), account.getAvatarUrl(),
                account.getRole(), account.getCreditScore(), account.getRealName(),
                account.getIdCardNo(), account.getRealNameStatus());
    }

    private List<AddressResponse> findAddresses(Long userId) {
        return jdbc.query("""
                SELECT id, receiver_name, receiver_phone, detail_address, is_default, created_at
                FROM user_addresses
                WHERE user_id = ?
                ORDER BY is_default DESC, created_at DESC, id DESC
                """, (rs, rowNum) -> new AddressResponse(
                        rs.getLong("id"),
                        rs.getString("receiver_name"),
                        rs.getString("receiver_phone"),
                        rs.getString("detail_address"),
                        rs.getInt("is_default") == 1,
                        rs.getTimestamp("created_at").toLocalDateTime()), userId);
    }

    private void clearDefaultAddress(Long userId) {
        jdbc.update("UPDATE user_addresses SET is_default = 0 WHERE user_id = ?", userId);
    }

    private void ensureAddressOwner(Long id, Long userId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_addresses WHERE id = ? AND user_id = ?",
                Integer.class, id, userId);
        if (count == null || count == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
