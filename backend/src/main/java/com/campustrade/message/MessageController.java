package com.campustrade.message;

import static com.campustrade.message.MessageDtos.*;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.campustrade.common.ApiResponse;
import com.campustrade.security.SecurityUser;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService service;
    public MessageController(MessageService service) { this.service = service; }
    @GetMapping public ApiResponse<List<MessageView>> list(Authentication auth) {
        return ApiResponse.success(service.list(userId(auth)));
    }
    @PostMapping public ApiResponse<MessageView> send(@Valid @RequestBody SendRequest request, Authentication auth) {
        return ApiResponse.success(service.send(userId(auth), request));
    }
    @PostMapping("/{id}/read") public ApiResponse<MessageView> read(@PathVariable long id, Authentication auth) {
        return ApiResponse.success(service.read(userId(auth), id));
    }
    private long userId(Authentication auth) { return ((SecurityUser) auth.getPrincipal()).userId(); }
}
