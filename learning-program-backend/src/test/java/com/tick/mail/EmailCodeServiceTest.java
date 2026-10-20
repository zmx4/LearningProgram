package com.tick.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailCodeServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final MailDispatcher dispatcher = mock(MailDispatcher.class);

    private final MailProperties properties = new MailProperties();
    private EmailCodeService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        service = new EmailCodeService(redis, properties, dispatcher);
    }

    @Test
    void sendsCodeAndStoresItWithTheConfiguredTtl() {
        String result = service.sendCode("user@example.test", MailType.REGISTER);

        assertNull(result);
        ArgumentCaptor<String> storedCode = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(EmailCodeService.codeKey(MailType.REGISTER, "user@example.test")),
                storedCode.capture(), eq(300L), eq(TimeUnit.SECONDS));
        assertEquals(6, storedCode.getValue().length());
        assertTrue(storedCode.getValue().matches("\\d{6}"), storedCode.getValue());

        ArgumentCaptor<EmailMessage> message = ArgumentCaptor.forClass(EmailMessage.class);
        verify(dispatcher).dispatch(message.capture());
        assertEquals("user@example.test", message.getValue().getTo());
        assertEquals(storedCode.getValue(), message.getValue().getCode());
        assertTrue(message.getValue().getContent().contains(storedCode.getValue()));
    }

    @Test
    void rejectsSecondRequestWithinTheInterval() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        String result = service.sendCode("user@example.test", MailType.REGISTER);

        assertTrue(result.contains("过于频繁"), result);
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void registerAndResetCodesUseDifferentKeys() {
        assertNotEquals(
                EmailCodeService.codeKey(MailType.REGISTER, "user@example.test"),
                EmailCodeService.codeKey(MailType.RESET, "user@example.test"));
        assertTrue(EmailCodeService.codeKey(MailType.REGISTER, "user@example.test").contains("register"));
        assertTrue(EmailCodeService.codeKey(MailType.RESET, "user@example.test").contains("reset"));
    }

    @Test
    void verifyAcceptsMatchingCodeAndRejectsWrongOrMissingOne() {
        when(valueOperations.get(EmailCodeService.codeKey(MailType.RESET, "user@example.test")))
                .thenReturn("123456");

        assertNull(service.verify("user@example.test", MailType.RESET, "123456"));
        assertNull(service.verify("user@example.test", MailType.RESET, " 123456 "));
        assertEquals("验证码有误", service.verify("user@example.test", MailType.RESET, "000000"));

        when(valueOperations.get(EmailCodeService.codeKey(MailType.RESET, "nobody@example.test")))
                .thenReturn(null);
        assertEquals("请先获取验证码", service.verify("nobody@example.test", MailType.RESET, "123456"));
    }

    @Test
    void verifyAndConsumeDeletesTheCodeOnlyWhenItMatches() {
        String key = EmailCodeService.codeKey(MailType.REGISTER, "user@example.test");
        when(valueOperations.get(key)).thenReturn("654321");

        assertEquals("验证码有误", service.verifyAndConsume("user@example.test", MailType.REGISTER, "111111"));
        verify(redis, never()).delete(key);

        assertNull(service.verifyAndConsume("user@example.test", MailType.REGISTER, "654321"));
        verify(redis).delete(key);
    }

    @Test
    void blankEmailIsRejectedBeforeTouchingRedis() {
        assertEquals("请填写邮箱", service.sendCode("   ", MailType.REGISTER));
        verify(dispatcher, never()).dispatch(any());
    }

    @Test
    void emptyCodeNeverMatchesAStoredOne() {
        when(valueOperations.get(anyString())).thenReturn("123456");

        assertEquals("验证码有误", service.verify("user@example.test", MailType.REGISTER, null));
        assertEquals("验证码有误", service.verify("user@example.test", MailType.REGISTER, ""));
    }
}
