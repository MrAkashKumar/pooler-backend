package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.config.AppProperties;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.exception.MailDispatchException;
import com.akash.pooler_backend.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AppProperties props;

    @Override
    @Async("mailExecutor")
    public void sendPasswordResetMail(PbUserEntity pbUserEntity, String resetToken) {
        String resetLink = props.getBaseUrl() + "/reset-password?token=" + resetToken;
        Context ctx = buildContext(pbUserEntity, Map.of(
                "resetLink",resetLink,
                "expiryMinutes", props.getPasswordReset().getTokenExpiryMinutes(),
                "username", pbUserEntity.getFirstName()
        ));
        sendHtmlMail(pbUserEntity.getEmail(), "Reset Your Password", "mail/password-reset", ctx);
        log.info("Password reset mail dispatched to {}", pbUserEntity.getEmail());

    }

    @Override
    @Async("mailExecutor")
    public void sendWelcomeMail(PbUserEntity pbUserEntity) {
        Context ctx = buildContext(pbUserEntity, Map.of(
                "userName",  pbUserEntity.getFirstName(),
                "loginLink", props.getBaseUrl() + "/login"
        ));
        sendHtmlMail(pbUserEntity.getEmail(), "Welcome to " + props.getName(), "mail/welcome", ctx);
        log.info("Welcome mail dispatched to {}", pbUserEntity.getEmail());

    }

    @Override
    @Async("mailExecutor")
    public void sendAccountLockedMail(PbUserEntity pbUserEntity) {
        Context ctx = buildContext(pbUserEntity, Map.of(
                "userName", pbUserEntity.getFirstName(),
                "lockMinutes", props.getSecurity().getLockDurationMinutes(),
                "supportEmail", props.getMail().getFrom()
        ));
        sendHtmlMail(pbUserEntity.getEmail(), "Account Security Alert", "mail/account-locked", ctx);
        log.info("Account locked mail dispatched to {}", pbUserEntity.getEmail());

    }

    // ── Generic mail dispatcher ────────────────────────────────────────

    private void sendHtmlMail(String to, String subject, String template, Context ctx) {
        try {
            String html = templateEngine.process(template, ctx);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getMail().getFrom(), props.getMail().getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send mail to {}: {}", to, e.getMessage());
            throw new MailDispatchException("Failed to send mail to: " + to, e);
        }
    }

    private Context buildContext(PbUserEntity pbUserEntity, Map<String, Object> extras) {
        Context ctx = new Context();
        ctx.setVariable("pbUserEntity", pbUserEntity);
        ctx.setVariable("appName", props.getName());
        ctx.setVariable("baseUrl", props.getBaseUrl());
        extras.forEach(ctx::setVariable);
        return ctx;
    }
}
