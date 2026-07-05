package com.campustrade.product;

import java.math.BigDecimal;

public record ProductSummary(
        Long id,
        String title,
        String category,
        BigDecimal price,
        String condition,
        String sellerNickname,
        String coverUrl) {
}
