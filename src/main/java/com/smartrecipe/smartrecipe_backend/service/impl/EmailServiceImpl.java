package com.smartrecipe.smartrecipe_backend.service.impl;

import com.smartrecipe.smartrecipe_backend.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:noreply@smartrecipe.com}")
    private String senderEmail;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otp) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();

        if (mailSender != null && senderEmail != null && !senderEmail.isBlank() && !"your-email@gmail.com".equals(senderEmail)) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(senderEmail, "Smart Recipe");
                helper.setTo(toEmail);
                helper.setSubject("Mã xác thực khôi phục mật khẩu - Smart Recipe");


                String htmlContent = buildOtpEmailContent(otp);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("Email OTP đã được gửi thành công đến: {}", toEmail);
                return;
            } catch (Exception e) {
                log.warn("Không thể gửi email thực tế qua SMTP ({}). Kích hoạt chế độ ghi log OTP dự phòng cho môi trường dev.", e.getMessage());
            }
        } else {
            log.info("Chưa cấu hình tài khoản Gmail SMTP hợp lệ. Chuyển sang chế độ Fallback Console Log.");
        }

        // Chế độ Dev Fallback: In mã OTP nổi bật ra màn hình console để dev/test luồng
        printConsoleFallbackOtp(toEmail, otp);
    }

    private void printConsoleFallbackOtp(String toEmail, String otp) {
        log.warn("\n===============================================================\n" +
                "📧 [SMART RECIPE - DEV OTP FALLBACK MOCK]\n" +
                "Người nhận: {}\n" +
                "Mã OTP xác thực: >>> {} <<<\n" +
                "Thời hạn: 10 phút\n" +
                "Lưu ý: Môi trường test chưa có App Password Gmail thật, dùng mã trên để tiếp tục kiểm thử.\n" +
                "===============================================================", toEmail, otp);
    }

    private String buildOtpEmailContent(String otp) {
        return "<div style=\"max-width: 540px; margin: 0 auto; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #ffffff; border: 1px solid #f0e6e4; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.05);\">" +
                "  <div style=\"background: linear-gradient(135deg, #a13923 0%, #c44c33 100%); padding: 28px 24px; text-align: center;\">" +
                "    <h1 style=\"color: #ffffff; margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px;\">🍳 Smart Recipe</h1>" +
                "    <p style=\"color: #fce8e4; margin: 6px 0 0 0; font-size: 14px;\">Nền tảng Quản lý Thực phẩm & Gợi ý Món ăn Thông minh</p>" +
                "  </div>" +
                "  <div style=\"padding: 32px 24px; color: #333333; line-height: 1.6;\">" +
                "    <h2 style=\"font-size: 18px; font-weight: 600; color: #a13923; margin-top: 0;\">Yêu cầu khôi phục mật khẩu</h2>" +
                "    <p>Xin chào bạn,</p>" +
                "    <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản Smart Recipe gắn với email này. Dưới đây là mã xác thực OTP của bạn:</p>" +
                "    <div style=\"margin: 28px 0; text-align: center;\">" +
                "      <div style=\"display: inline-block; background-color: #fff2ef; border: 2px dashed #a13923; border-radius: 10px; padding: 14px 28px; font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #a13923;\">" +
                otp +
                "      </div>" +
                "    </div>" +
                "    <p style=\"font-size: 13px; color: #777777;\">⏱ Mã này có hiệu lực trong vòng <strong>10 phút</strong>. Vì lý do an toàn, vui lòng không chia sẻ mã này cho bất kỳ ai.</p>" +
                "    <p style=\"font-size: 13px; color: #777777; margin-bottom: 0;\">Nếu bạn không thực hiện yêu cầu này, hãy yên tâm bỏ qua email này hoặc đổi mật khẩu hiện tại.</p>" +
                "  </div>" +
                "  <div style=\"background-color: #fdfaf9; padding: 16px 24px; text-align: center; border-top: 1px solid #f2e9e7; font-size: 12px; color: #999999;\">" +
                "    © 2026 Smart Recipe Platform. All rights reserved." +
                "  </div>" +
                "</div>";
    }
}
