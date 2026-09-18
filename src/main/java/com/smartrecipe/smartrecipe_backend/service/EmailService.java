package com.smartrecipe.smartrecipe_backend.service;

/**
 * Service xử lý việc gửi email thông báo và mã OTP.
 */
public interface EmailService {

    /**
     * Gửi email chứa mã OTP khôi phục mật khẩu đến người dùng.
     *
     * @param toEmail Địa chỉ email người nhận
     * @param otp Mã xác thực 6 chữ số
     */
    void sendOtpEmail(String toEmail, String otp);
}
