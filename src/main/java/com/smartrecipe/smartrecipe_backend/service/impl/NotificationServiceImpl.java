package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.response.AuthorSummaryResponse;
import com.smartrecipe.smartrecipe_backend.dto.response.NotificationResponse;
import com.smartrecipe.smartrecipe_backend.entity.Notification;
import com.smartrecipe.smartrecipe_backend.entity.Recipe;
import com.smartrecipe.smartrecipe_backend.entity.RecipeComment;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.enums.NotificationType;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.NotificationRepository;
import com.smartrecipe.smartrecipe_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageRequest)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo: " + notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền sửa thông báo này");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByRecipientId(userId);
    }

    @Override
    public void createNotification(User recipient, User actor, Recipe recipe, RecipeComment comment, NotificationType type, String message) {
        if (recipient == null || actor == null) return;
        // Tránh tự gửi thông báo cho chính mình
        if (recipient.getId().equals(actor.getId())) return;

        Notification notification = Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .recipe(recipe)
                .comment(comment)
                .type(type)
                .message(message)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public void createNotificationSafe(User recipient, User actor, Recipe recipe, RecipeComment comment, NotificationType type, String message) {
        // Delegate — self-notification guard is already in createNotification
        createNotification(recipient, actor, recipe, comment, type, message);
    }

    private NotificationResponse mapToResponse(Notification n) {
        AuthorSummaryResponse actorResponse = null;
        if (n.getActor() != null) {
            actorResponse = AuthorSummaryResponse.builder()
                    .id(n.getActor().getId())
                    .username(n.getActor().getUsername())
                    .displayName(n.getActor().getDisplayName())
                    .avatarUrl(n.getActor().getAvatarUrl())
                    .build();
        }

        return NotificationResponse.builder()
                .id(n.getId())
                .actor(actorResponse)
                .recipeId(n.getRecipe() != null ? n.getRecipe().getId() : null)
                .recipeTitle(n.getRecipe() != null ? n.getRecipe().getTitle() : null)
                .commentId(n.getComment() != null ? n.getComment().getId() : null)
                .type(n.getType())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
