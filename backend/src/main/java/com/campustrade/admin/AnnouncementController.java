package com.campustrade.admin;

import static com.campustrade.admin.AdminDtos.AnnouncementView;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.campustrade.common.ApiResponse;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AdminService service;
    public AnnouncementController(AdminService service) { this.service = service; }
    @GetMapping public ApiResponse<List<AnnouncementView>> list() { return ApiResponse.success(service.published()); }
}
