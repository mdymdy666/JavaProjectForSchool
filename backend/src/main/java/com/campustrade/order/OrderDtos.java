package com.campustrade.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class OrderDtos {
    private OrderDtos() {}

    public record CreateOrderRequest(@NotNull Long productId) {}
    public record ShipRequest(@NotBlank @Size(max = 255) String logisticsInfo) {}
    public record OrderView(
            Long id, String orderNo, Long productId, String productTitle,
            Long buyerId, String buyerNickname, Long sellerId, String sellerNickname,
            BigDecimal totalAmount, String status, String logisticsInfo,
            Integer version, LocalDateTime createdAt) {}
}
