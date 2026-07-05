package com.campustrade.admin;

import static com.campustrade.admin.AdminDtos.*;
import org.springframework.web.bind.annotation.*;
import com.campustrade.common.ApiResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService service;
    public AdminController(AdminService service) { this.service = service; }
    @GetMapping("/dashboard") public ApiResponse<DashboardView> dashboard() {
        return ApiResponse.success(service.dashboard());
    }
    @PostMapping("/announcements") public ApiResponse<AnnouncementView> create(
            @Valid @RequestBody AnnouncementRequest request) {
        return ApiResponse.success(service.create(request));
    }
    @DeleteMapping("/announcements/{id}") public ApiResponse<Void> delete(@PathVariable long id) {
        service.delete(id); return ApiResponse.success(null);
    }
}
