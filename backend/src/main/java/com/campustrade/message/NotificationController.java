package com.campustrade.message;

import static com.campustrade.message.MessageDtos.NotificationView;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.campustrade.common.ApiResponse;
import com.campustrade.security.SecurityUser;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }
    @GetMapping public ApiResponse<List<NotificationView>> list(Authentication auth) {
        return ApiResponse.success(service.list(userId(auth)));
    }
    @PostMapping("/{id}/read") public ApiResponse<NotificationView> read(@PathVariable long id, Authentication auth) {
        return ApiResponse.success(service.read(userId(auth), id));
    }
    private long userId(Authentication auth) { return ((SecurityUser) auth.getPrincipal()).userId(); }
}
