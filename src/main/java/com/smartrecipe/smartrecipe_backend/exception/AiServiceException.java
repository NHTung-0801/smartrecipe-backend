package com.smartrecipe.smartrecipe_backend.exception;

/**
 * Exception cho các lỗi phát sinh khi gọi nhà cung cấp AI bên ngoài (Gemini):
 * timeout, mất mạng, HTTP 4xx/5xx, hoặc response không đúng cấu trúc mong đợi.
 *
 * <p>Tách riêng khỏi {@link BadRequestException} vì đây KHÔNG phải lỗi do người
 * dùng nhập sai — người dùng chỉ cần thử lại. GlobalExceptionHandler trả về
 * HTTP 503 Service Unavailable để frontend phân biệt được với 400.
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
