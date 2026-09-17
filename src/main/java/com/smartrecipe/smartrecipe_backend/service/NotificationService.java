package com.smartrecipe.smartrecipe_backend.service;

import com.smartrecipe.smartrecipe_backend.dto.response.NotificationResponse;
import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.entity.RecipeComment;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.enums.NotificationType;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getNotifications(Long userId, int page, int size);
    long getUnreadCount(Long userId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
    void createNotification(User recipient, User actor, Recipe recipe, RecipeComment comment, NotificationType type, String message);
    /** Như createNotification nhưng bỏ qua nếu actor == recipient (tránh self-notification) */
    void createNotificationSafe(User recipient, User actor, Recipe recipe, RecipeComment comment, NotificationType type, String message);
}
