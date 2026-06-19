package com.geoattend.utils;

import android.util.Log;
import com.geoattend.BuildConfig;
import java.util.Properties;
import javax.mail.Authenticator;
import android.os.Handler;
import android.os.Looper;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * A free and private SMTP mailer for sending OTPs directly from the app.
 * Security: Use an "App Password" from your email provider, not your login password.
 */
public class SmtpMailer {

    // These are now securely loaded from local.properties via BuildConfig
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SENDER_EMAIL = BuildConfig.SENDER_EMAIL;
    private static final String APP_PASSWORD = BuildConfig.APP_PASSWORD;

    public interface MailCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void sendOtpEmail(String recipientEmail, String otp, MailCallback callback) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject("GeoAttend - Verification Code");
                
                String htmlContent = "<div style='font-family: sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 10px;'>" +
                        "<h2 style='color: #2563EB;'>Security Verification</h2>" +
                        "<p>Please use the following 6-digit code to complete your registration:</p>" +
                        "<div style='font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #111; margin: 20px 0;'>" + otp + "</div>" +
                        "<p style='color: #666; font-size: 12px;'>This code will expire in 10 minutes. If you didn't request this, ignore this email.</p>" +
                        "</div>";

                message.setContent(htmlContent, "text/html; charset=utf-8");

                Transport.send(message);
                
                new Handler(Looper.getMainLooper()).post(callback::onSuccess);
                
            } catch (Exception e) {
                Log.e("SmtpMailer", "Failed to send email", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(e));
            }
        }).start();
    }
}
