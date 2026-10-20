package com.tick.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 真正把邮件「送出去」的地方，两种方式：
 * <ul>
 *   <li>{@code delivery=log}：只写日志（开发默认）。验证码会以 {@code code=xxxxxx} 的形式出现，
 *       本地联调时直接看日志即可，不需要任何 SMTP 账号。</li>
 *   <li>{@code delivery=smtp}：走 {@link JavaMailSender} 真实发信。</li>
 * </ul>
 * 这个方法**永远不会抛异常**：SMTP 没配好或发送失败都只会降级成日志，
 * 免得用户拿不到验证码、接口却报 500。
 */
@Component
public class MailDelivery {
    private static final Logger log = LoggerFactory.getLogger(MailDelivery.class);

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final MailProperties properties;

    public MailDelivery(ObjectProvider<JavaMailSender> javaMailSenderProvider, MailProperties properties) {
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.properties = properties;
    }

    public void send(EmailMessage message) {
        if ("smtp".equalsIgnoreCase(properties.getDelivery()) && sendBySmtp(message)) {
            return;
        }
        logByConsole(message);
    }

    private boolean sendBySmtp(EmailMessage message) {
        JavaMailSender mailSender = javaMailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("learning.mail.delivery=smtp，但没有配置 spring.mail.host，改为写日志");
            return false;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(properties.getFrom());
            mail.setTo(message.getTo());
            mail.setSubject(message.getSubject());
            mail.setText(message.getContent());
            mailSender.send(mail);
            log.info("邮件已通过 SMTP 发送：to={} subject={}", message.getTo(), message.getSubject());
            return true;
        } catch (Exception exception) {
            log.warn("SMTP 发送失败，改为写日志：to={} 原因={}", message.getTo(), exception.getMessage());
            return false;
        }
    }

    private void logByConsole(EmailMessage message) {
        // 开发模式下验证码只能从这里拿到，格式保持醒目、便于 grep
        log.info("[mail:log] to={} subject={} code={}\n{}",
                message.getTo(), message.getSubject(),
                message.getCode() == null ? "-" : message.getCode(),
                message.getContent());
    }
}
