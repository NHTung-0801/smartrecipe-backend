package com.smartrecipe.smartrecipe_backend.dto.response;

import com.smartrecipe.smartrecipe_backend.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private AuthorSummaryResponse actor;
    private Long recipeId;
    private String recipeTitle;
    private Long commentId;
    private NotificationType type;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
