package com.smartrecipe.smartrecipe_backend.exception;

/**
 * Exception được throw khi user vượt quá giới hạn gọi API trong ngày.
 * Sẽ được GlobalExceptionHandler bắt và trả về HTTP 429 Too Many Requests.
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
