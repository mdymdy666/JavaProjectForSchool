package com.campustrade.message;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class MessageDtos {
    private MessageDtos() {}
    public record SendRequest(@NotNull Long receiverId, @NotNull Long productId,
            @NotBlank @Size(max = 500) String content) {}
    public record MessageView(Long id, Long senderId, String senderNickname,
            Long receiverId, String receiverNickname, Long productId, String productTitle,
            String content, String readStatus, LocalDateTime createdAt) {}
    public record NotificationView(Long id, String type, String title, String content,
            String readStatus, LocalDateTime createdAt) {}
}
