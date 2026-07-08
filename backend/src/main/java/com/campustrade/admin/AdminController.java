package com.campustrade.admin;

import static com.campustrade.admin.AdminDtos.*;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.campustrade.common.ApiResponse;
import com.campustrade.common.PageResult;
import com.campustrade.extension.ContentReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;
    private final ContentReviewService contentReview;
    public AdminController(AdminService service, ContentReviewService contentReview) {
        this.service = service;
        this.contentReview = contentReview;
    }

    // Dashboard + stats
    @GetMapping("/dashboard") public ApiResponse<DashboardView> dashboard() {
        return ApiResponse.success(service.dashboard());
    }
    @GetMapping("/trend") public ApiResponse<List<AdminService.TrendPoint>> trend() {
        return ApiResponse.success(service.weekTrend());
    }

    // Announcements
    @PostMapping("/announcements") public ApiResponse<AnnouncementView> create(
            @Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.success(service.create(request));
    }
    @DeleteMapping("/announcements/{id}") public ApiResponse<Void> delete(@PathVariable long id) {
        service.delete(id); return ApiResponse.success(null);
    }

    // User management
    @GetMapping("/users")
    public ApiResponse<PageResult<AdminService.UserRow>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        AdminService.UserPage p = service.listUsers(keyword, page, size);
        return ApiResponse.success(new PageResult<>(p.records(), p.total(), page, size));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<AdminService.UserRow> toggleUserStatus(@PathVariable long id) {
        return ApiResponse.success(service.toggleUserStatus(id));
    }

    // Order management
    @GetMapping("/orders")
    public ApiResponse<PageResult<AdminService.AdminOrderRow>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        AdminService.AdminOrderPage p = service.listAllOrders(status, page, size);
        return ApiResponse.success(new PageResult<>(p.records(), p.total(), page, size));
    }

    // Content review
    public record SensitiveWordRequest(String word) {}
    public record SensitiveWordStatusRequest(boolean enabled) {}

    @GetMapping("/sensitive-words")
    public ApiResponse<List<ContentReviewService.SensitiveWordView>> sensitiveWords() {
        return ApiResponse.success(contentReview.listWords());
    }

    @PostMapping("/sensitive-words")
    public ApiResponse<ContentReviewService.SensitiveWordView> addSensitiveWord(
            @RequestBody SensitiveWordRequest request) {
        return ApiResponse.success(contentReview.addWord(request.word()));
    }

    @PutMapping("/sensitive-words/{id}/status")
    public ApiResponse<Void> setSensitiveWordStatus(
            @PathVariable long id,
            @RequestBody SensitiveWordStatusRequest request) {
        contentReview.setEnabled(id, request.enabled());
        return ApiResponse.success(null);
    }
}
