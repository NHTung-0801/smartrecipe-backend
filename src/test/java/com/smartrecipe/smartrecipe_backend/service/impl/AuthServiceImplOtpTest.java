package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.ForgotPasswordRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.ResetPasswordRequest;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.security.JwtProvider;
import com.smartrecipe.smartrecipe_backend.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplOtpTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtProvider jwtProvider;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private EmailService emailService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtProvider,
                stringRedisTemplate,
                emailService
        );
    }

    @Test
    @DisplayName("forgotPassword: Gửi mã OTP thành công khi email hợp lệ và không có cooldown")
    void forgotPassword_Success() {
        String email = "test@example.com";
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        User user = User.builder().id(1L).email(email).username("testuser").build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(stringRedisTemplate.hasKey("otp:cooldown:" + email)).thenReturn(false);

        authService.forgotPassword(request);

        // Xác nhận đã lưu OTP vào Redis với TTL 10 phút
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("otp:reset:" + email), otpCaptor.capture(), eq(10L), eq(TimeUnit.MINUTES));
        assertThat(otpCaptor.getValue()).matches("^\\d{6}$");

        // Xác nhận đã lưu cooldown 60 giây
        verify(valueOperations).set(eq("otp:cooldown:" + email), eq("1"), eq(60L), eq(TimeUnit.SECONDS));

        // Xác nhận đã gọi EmailService
        verify(emailService).sendOtpEmail(eq(email), eq(otpCaptor.getValue()));
    }

    @Test
    @DisplayName("forgotPassword: Báo lỗi ResourceNotFoundException khi email không tồn tại trong hệ thống")
    void forgotPassword_UserNotFound() {
        String email = "unknown@example.com";
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy tài khoản");

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("forgotPassword: Báo lỗi BadRequestException khi yêu cầu lại quá nhanh (cooldown 60s)")
    void forgotPassword_CooldownActive() {
        String email = "test@example.com";
        ForgotPasswordRequest request = new ForgotPasswordRequest(email);
        User user = User.builder().id(1L).email(email).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(stringRedisTemplate.hasKey("otp:cooldown:" + email)).thenReturn(true);
        when(stringRedisTemplate.getExpire("otp:cooldown:" + email, TimeUnit.SECONDS)).thenReturn(45L);

        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("45 giây");

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("resetPassword: Đặt lại mật khẩu thành công khi OTP chính xác")
    void resetPassword_Success() {
        String email = "test@example.com";
        String otp = "123456";
        String newPassword = "NewSecurePassword123!";
        ResetPasswordRequest request = new ResetPasswordRequest(email, otp, newPassword, newPassword);

        User user = User.builder().id(1L).email(email).passwordHash("old_hash").build();

        when(valueOperations.get("otp:attempts:" + email)).thenReturn(null);
        when(valueOperations.get("otp:reset:" + email)).thenReturn(otp);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("new_hash");

        authService.resetPassword(request);

        // Mật khẩu mới đã được băm và lưu
        assertThat(user.getPasswordHash()).isEqualTo("new_hash");
        verify(userRepository).save(user);

        // Dọn dẹp key redis
        verify(stringRedisTemplate).delete(anyList());
    }

    @Test
    @DisplayName("resetPassword: Báo lỗi khi mật khẩu xác nhận không khớp")
    void resetPassword_ConfirmPasswordMismatch() {
        String email = "test@example.com";
        ResetPasswordRequest request = new ResetPasswordRequest(email, "123456", "NewPassword123", "MismatchPassword456");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không khớp");

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("resetPassword: Báo lỗi khi mã OTP đã hết hạn hoặc không tồn tại trong Redis")
    void resetPassword_OtpExpired() {
        String email = "test@example.com";
        ResetPasswordRequest request = new ResetPasswordRequest(email, "123456", "NewPassword123", "NewPassword123");

        when(valueOperations.get("otp:attempts:" + email)).thenReturn(null);
        when(valueOperations.get("otp:reset:" + email)).thenReturn(null);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("hết hạn");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("resetPassword: Báo lỗi khi mã OTP không khớp và tăng số lần thử")
    void resetPassword_WrongOtp() {
        String email = "test@example.com";
        ResetPasswordRequest request = new ResetPasswordRequest(email, "999999", "NewPassword123", "NewPassword123");

        when(valueOperations.get("otp:attempts:" + email)).thenReturn("1");
        when(valueOperations.get("otp:reset:" + email)).thenReturn("123456");
        when(valueOperations.increment("otp:attempts:" + email)).thenReturn(2L);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mã OTP không chính xác. Bạn còn 3 lần thử.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("resetPassword: Khóa và xóa OTP khi nhập sai quá 5 lần (Brute-force protection)")
    void resetPassword_MaxAttemptsExceeded() {
        String email = "test@example.com";
        ResetPasswordRequest request = new ResetPasswordRequest(email, "999999", "NewPassword123", "NewPassword123");

        when(valueOperations.get("otp:attempts:" + email)).thenReturn("5");

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("quá 5 lần");

        verify(stringRedisTemplate).delete("otp:reset:" + email);
        verify(userRepository, never()).save(any());
    }
}

