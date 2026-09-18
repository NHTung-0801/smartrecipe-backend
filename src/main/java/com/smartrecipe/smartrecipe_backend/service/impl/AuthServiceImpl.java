package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.dto.request.ForgotPasswordRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.LoginRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RefreshTokenRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.RegisterRequest;
import com.smartrecipe.smartrecipe_backend.dto.request.ResetPasswordRequest;
import com.smartrecipe.smartrecipe_backend.dto.response.AuthResponse;
import com.smartrecipe.smartrecipe_backend.entity.User;
import com.smartrecipe.smartrecipe_backend.enums.Role;
import com.smartrecipe.smartrecipe_backend.exception.BadRequestException;
import com.smartrecipe.smartrecipe_backend.exception.DuplicateResourceException;
import com.smartrecipe.smartrecipe_backend.exception.ResourceNotFoundException;
import com.smartrecipe.smartrecipe_backend.repository.UserRepository;
import com.smartrecipe.smartrecipe_backend.security.JwtProvider;
import com.smartrecipe.smartrecipe_backend.service.AuthService;
import com.smartrecipe.smartrecipe_backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();


    @Override
    public void register(RegisterRequest registerRequest) {
        // Kiểm tra xem Username đã tồn tại chưa
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new DuplicateResourceException("Username đã được sử dụng!");
        }

        // Kiểm tra xem Email đã tồn tại chưa
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateResourceException("Email đã được sử dụng!");
        }

        // Tạo User mới và băm mật khẩu
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .role(Role.USER) // Mặc định tất cả user mới đăng ký đều có quyền USER
                .build();

        userRepository.save(user);
    }

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        // Ủy quyền cho Spring Security xác thực username và password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        // Set context cho phiên hiện tại
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Lấy thông tin user (đã được xác thực nên chắc chắn tồn tại)
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .or(() -> userRepository.findByEmail(loginRequest.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User"));

        // Sinh Access Token & Refresh Token
        String jwt = jwtProvider.generateToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Xác thực refresh token
        if (!jwtProvider.validateJwtToken(refreshToken)) {
            throw new BadRequestException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        // Lấy username từ refresh token
        String username = jwtProvider.getUserNameFromJwtToken(refreshToken);

        // Kiểm tra user có tồn tại không
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User"));

        // Sinh cặp token mới
        String newAccessToken = jwtProvider.generateTokenFromUsername(username);
        String newRefreshToken = jwtProvider.generateRefreshToken(username);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public void logout(String username) {
        // Kiểm tra user có tồn tại không
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy User"));

        // Xóa SecurityContext
        SecurityContextHolder.clearContext();

        // TODO: Sau này tích hợp Redis — thêm refresh token vào blacklist
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Kiểm tra tài khoản có tồn tại không
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản liên kết với email: " + email));

        // 2. Chống spam yêu cầu liên tục (Cooldown 60 giây)
        String cooldownKey = "otp:cooldown:" + email;
        Boolean hasCooldown = stringRedisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(hasCooldown)) {
            Long expire = stringRedisTemplate.getExpire(cooldownKey, TimeUnit.SECONDS);
            throw new BadRequestException("Vui lòng đợi " + (expire != null ? expire : 60) + " giây trước khi gửi lại yêu cầu mã mới.");
        }

        // 3. Sinh mã OTP ngẫu nhiên 6 chữ số
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        // 4. Lưu mã OTP vào Redis (TTL 10 phút) và thiết lập cooldown 60 giây
        String otpKey = "otp:reset:" + email;
        stringRedisTemplate.opsForValue().set(otpKey, otp, 10, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", 60, TimeUnit.SECONDS);
        stringRedisTemplate.delete("otp:attempts:" + email);

        // 5. Gửi email chứa mã OTP
        emailService.sendOtpEmail(email, otp);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // Kiểm tra mật khẩu xác nhận
        if (request.getConfirmPassword() != null && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp với mật khẩu mới.");
        }

        String email = request.getEmail().trim().toLowerCase();
        String otpKey = "otp:reset:" + email;
        String attemptsKey = "otp:attempts:" + email;
        String cooldownKey = "otp:cooldown:" + email;


        // 1. Kiểm tra số lần nhập sai (Brute-force protection: tối đa 5 lần)
        String attemptsStr = stringRedisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= 5) {
            stringRedisTemplate.delete(otpKey);
            throw new BadRequestException("Bạn đã nhập sai mã OTP quá 5 lần. Mã xác thực đã bị vô hiệu hóa, vui lòng gửi lại yêu cầu mới.");
        }

        // 2. Lấy mã OTP từ Redis
        String cachedOtp = stringRedisTemplate.opsForValue().get(otpKey);
        if (cachedOtp == null) {
            throw new BadRequestException("Mã OTP không tồn tại hoặc đã hết hạn (10 phút). Vui lòng gửi lại yêu cầu mới.");
        }

        // 3. So khớp mã OTP
        if (!cachedOtp.equals(request.getOtp().trim())) {
            Long newAttempts = stringRedisTemplate.opsForValue().increment(attemptsKey);
            stringRedisTemplate.expire(attemptsKey, 10, TimeUnit.MINUTES);
            int remaining = 5 - (newAttempts != null ? newAttempts.intValue() : 1);
            if (remaining <= 0) {
                stringRedisTemplate.delete(otpKey);
                throw new BadRequestException("Bạn đã nhập sai mã OTP quá 5 lần. Mã xác thực đã bị vô hiệu hóa.");
            }
            throw new BadRequestException("Mã OTP không chính xác. Bạn còn " + remaining + " lần thử.");
        }

        // 4. Cập nhật mật khẩu mới cho tài khoản
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản người dùng liên kết với email: " + email));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 5. Thu hồi OTP và dọn dẹp Redis keys (đảm bảo single-use)
        stringRedisTemplate.delete(List.of(otpKey, attemptsKey, cooldownKey));
    }
}
