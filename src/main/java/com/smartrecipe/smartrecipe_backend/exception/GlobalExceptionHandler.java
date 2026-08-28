package com.smartrecipe.smartrecipe_backend.exception;

import com.smartrecipe.smartrecipe_backend.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), "NOT_FOUND"), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), "UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(DuplicateResourceException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), "CONFLICT"), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(org.springframework.security.authentication.BadCredentialsException ex) {
        return new ResponseEntity<>(ApiResponse.error("Tên đăng nhập hoặc mật khẩu không chính xác", "UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsernameNotFoundException(org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
        return new ResponseEntity<>(ApiResponse.error("Tên đăng nhập hoặc mật khẩu không chính xác", "UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(BadRequestException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceededException(RateLimitExceededException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), "TOO_MANY_REQUESTS"), HttpStatus.TOO_MANY_REQUESTS);
    }

    // Lỗi từ nhà cung cấp AI bên ngoài (timeout, 5xx, response sai cấu trúc).
    // Trả 503 chứ không phải 400/500: request của user hợp lệ, chỉ cần thử lại.
    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleAiServiceException(AiServiceException ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error(ex.getMessage(), "AI_SERVICE_UNAVAILABLE"), HttpStatus.SERVICE_UNAVAILABLE);
    }

    // @PreAuthorize từ chối -> phải là 403, không phải 500.
    // Nếu không bắt riêng thì handler Exception ở cuối sẽ biến nó thành
    // "Internal Server Error", frontend không phân biệt được thiếu quyền với lỗi thật.
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex) {
        return new ResponseEntity<>(
                ApiResponse.error("Bạn không có quyền thực hiện hành động này", "FORBIDDEN"),
                HttpStatus.FORBIDDEN);
    }

    // Xử lý lỗi Validate (Bean Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return new ResponseEntity<>(ApiResponse.error(message, "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
    }

    // Bắt toàn bộ lỗi còn lại để tránh văng Exception thô ra ngoài
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex, WebRequest request) {
        return new ResponseEntity<>(ApiResponse.error("Internal Server Error: " + ex.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
