package com.campustrade.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProductDtos {
    private ProductDtos() {
    }

    public record PublishRequest(
            @NotNull Long categoryId,
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 5000) String description,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotBlank @Size(max = 30) String itemCondition,
            @NotNull @Size(min = 1, max = 6) List<@NotBlank String> imageUrls) {
    }

    public record AuditRequest(boolean approved, @NotBlank @Size(max = 255) String reason) {
    }

    public record ProductCard(
            Long id,
            String title,
            BigDecimal price,
            String itemCondition,
            String status,
            Integer viewCount,
            String categoryName,
            String sellerNickname,
            String coverUrl,
            LocalDateTime createdAt) {
    }

    public record ProductDetail(
            Long id,
            Long sellerId,
            String sellerNickname,
            Long categoryId,
            String categoryName,
            String title,
            String description,
            BigDecimal price,
            String itemCondition,
            String status,
            Integer viewCount,
            List<String> images,
            boolean favorite,
            LocalDateTime createdAt) {
    }

    public record FavoriteResponse(Long productId, boolean favorite) {
    }
}
