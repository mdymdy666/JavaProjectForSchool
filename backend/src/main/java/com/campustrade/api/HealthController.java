package com.campustrade.api;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campustrade.common.ApiResponse;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<HealthStatus> health() {
        return ApiResponse.success(new HealthStatus(
                "UP",
                "campus-trade-backend",
                OffsetDateTime.now(),
                List.of("auth", "product", "order", "message", "admin")));
    }

    public record HealthStatus(
            String status,
            String application,
            OffsetDateTime checkedAt,
            List<String> plannedModules) {
    }
}
