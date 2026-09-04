package com.smartrecipe.smartrecipe_backend.enums;

public enum RecipeStatus {
    DRAFT,
    PENDING_REVIEW,   // Gửi lên chờ Admin duyệt
    PRIVATE,
    PUBLIC,
    DELETED
}
