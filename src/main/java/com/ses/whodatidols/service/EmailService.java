package com.ses.whodatidols.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @SuppressWarnings("null")
    public void sendVerificationCode(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("info@whodatidols.com");
            helper.setTo(email);
            helper.setSubject("Who Dat Idols - Şifre Sıfırlama Kodu");

            String htmlContent = buildVerificationEmailTemplate(code);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Doğrulama kodu gönderilirken hata oluştu: " + e.getMessage());
        }
    }

    private String buildVerificationEmailTemplate(String code) {
        return "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px;background:#222;color:#fff;border-radius:10px;'>"
                + "<div style='text-align:center;margin-bottom:30px;'>"
                + "<h1 style='color:#1ed760;margin:0;'>Who Dat Idols</h1>"
                + "<p style='color:#ccc;font-size:16px;margin-top:10px;'>Şifre Sıfırlama</p>"
                + "</div>"
                + "<p>Merhaba,</p>"
                + "<p>Şifrenizi sıfırlamak için aşağıdaki doğrulama kodunu kullanın:</p>"
                + "<div style='text-align:center;margin:30px 0;'>"
                + "<div style='display:inline-block;background:#1ed760;padding:15px 30px;border-radius:5px;letter-spacing:5px;font-size:28px;font-weight:bold;color:#000;'>"
                + code
                + "</div>"
                + "</div>"
                + "<p>Bu kod 2 dakika içinde geçerliliğini yitirecektir.</p>"
                + "<p>Eğer bir şifre sıfırlama talebinde bulunmadıysanız, bu e-postayı dikkate almayınız.</p>"
                + "<hr style='border:none;border-top:1px solid #444;margin:30px 0;'>"
                + "<p style='color:#999;font-size:14px;text-align:center;'>© 2025 Who Dat Idols? Tüm hakları saklıdır.</p>"
                + "</div>";
    }
}