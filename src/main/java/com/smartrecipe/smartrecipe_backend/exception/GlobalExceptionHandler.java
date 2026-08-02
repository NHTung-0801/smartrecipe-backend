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
