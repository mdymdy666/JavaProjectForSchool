package com.campustrade.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AdminDtos {
    private AdminDtos() {}
    public record CategoryStat(String categoryName, long productCount) {}
    public record DashboardView(long userCount, long productCount, long orderCount,
            BigDecimal turnover, List<CategoryStat> categories) {}
    public record AnnouncementRequest(@NotBlank @Size(max = 100) String title,
            @NotBlank String content, boolean published) {}
    public record AnnouncementView(Long id, String title, String content,
            boolean published, LocalDateTime createdAt) {}
}
