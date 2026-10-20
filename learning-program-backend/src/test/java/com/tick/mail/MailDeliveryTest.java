package com.tick.mail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 发信实现：smtp 正常发送、没配 SMTP 或发送失败时降级为日志，且永不抛异常。
 */
class MailDeliveryTest {

    private final MailProperties properties = new MailProperties();
    private final ObjectProvider<JavaMailSender> javaMailSenderProvider = mock(ObjectProvider.class);

    private final EmailMessage message = new EmailMessage("user@example.test", "主题", "正文 123456", "123456");

    private MailDelivery delivery() {
        return new MailDelivery(javaMailSenderProvider, properties);
    }

    @Test
    void logModeDoesNotTouchSmtp() {
        properties.setDelivery("log");
        when(javaMailSenderProvider.getIfAvailable()).thenReturn(mock(JavaMailSender.class));

        assertDoesNotThrow(() -> delivery().send(message));

        verify(javaMailSenderProvider.getIfAvailable(), never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void smtpModeUsesTheMailSender() {
        properties.setDelivery("smtp");
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(javaMailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        delivery().send(message);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void smtpWithoutConfigurationFallsBackInsteadOfThrowing() {
        properties.setDelivery("smtp");
        when(javaMailSenderProvider.getIfAvailable()).thenReturn(null);

        assertDoesNotThrow(() -> delivery().send(message));
    }

    @Test
    void smtpFailureFallsBackInsteadOfThrowing() {
        properties.setDelivery("smtp");
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(javaMailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> delivery().send(message));
    }
}
