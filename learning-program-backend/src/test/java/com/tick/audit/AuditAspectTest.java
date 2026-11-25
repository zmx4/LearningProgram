package com.tick.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tick.controller.AdminController;
import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.service.AccountService;
import com.tick.service.AdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditAspectTest {

    private final Logger auditLogger = (Logger) LoggerFactory.getLogger(AuditAspect.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level originalLevel;

    @BeforeEach
    void captureAuditLogs() {
        originalLevel = auditLogger.getLevel();
        auditLogger.setLevel(Level.INFO);
        appender.start();
        auditLogger.addAppender(appender);
    }

    @AfterEach
    void releaseAuditLogs() {
        auditLogger.detachAppender(appender);
        appender.stop();
        auditLogger.setLevel(originalLevel);
    }

    @Test
    void successfulAuditRecordsIdentifiersWithoutSensitiveValues() {
        AuditedService proxy = proxy(new AuditedService());

        String result = proxy.sensitiveOperation("secret-password", 7, 42, 99);

        assertEquals("sensitive-result", result);
        ILoggingEvent event = onlyEvent();
        String message = event.getFormattedMessage();
        assertEquals("AUDIT", event.getMarker().getName());
        assertTrue(message.contains("action=TEST_UPDATE"));
        assertTrue(message.contains("operatorId=7"));
        assertTrue(message.contains("resourceType=account"));
        assertTrue(message.contains("resourceId=42"));
        assertTrue(message.contains("targetId=99"));
        assertTrue(message.contains("outcome=SUCCESS"));
        assertFalse(message.contains("secret-password"));
        assertFalse(message.contains("sensitive-result"));
    }

    @Test
    void failedAuditRecordsExceptionTypeAndRethrows() {
        AuditedService proxy = proxy(new AuditedService());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> proxy.failingOperation(7, 42));

        assertEquals("sensitive-error", exception.getMessage());
        String message = onlyEvent().getFormattedMessage();
        assertTrue(message.contains("outcome=FAILED"));
        assertTrue(message.contains("errorType=IllegalStateException"));
        assertFalse(message.contains("sensitive-error"));
    }

    @Test
    void businessRejectionIsAuditedAsRejected() {
        AuditedService proxy = proxy(new AuditedService());

        RestBean<Void> response = proxy.rejectedOperation(7, 42);

        assertEquals(400, response.code());
        String message = onlyEvent().getFormattedMessage();
        assertTrue(message.contains("outcome=REJECTED"));
        assertFalse(message.contains("sensitive-message"));
    }

    @Test
    void unannotatedMethodIsNotAudited() {
        proxy(new AuditedService()).ordinaryOperation();

        assertTrue(appender.list.isEmpty());
    }

    @Test
    void selfInvocationBypassesAuditProxy() {
        proxy(new AuditedService()).selfInvokingOperation();

        assertTrue(appender.list.isEmpty());
    }

    @Test
    void selectedAdminControllerMethodIsAuditedThroughProxy() {
        AccountService accountService = mock(AccountService.class);
        Account account = mock(Account.class);
        when(accountService.getById(42)).thenReturn(account);
        when(account.getUsername()).thenReturn("target-user");
        when(accountService.resetPassword(42)).thenReturn("generated-password");
        AdminController controller = new AdminController(accountService, mock(AdminService.class));

        RestBean<Map<String, String>> response = proxy(controller).resetPassword(42, 7);

        assertEquals(200, response.code());
        verify(accountService).resetPassword(42);
        String message = onlyEvent().getFormattedMessage();
        assertTrue(message.contains("action=ADMIN_RESET_PASSWORD"));
        assertTrue(message.contains("operatorId=7"));
        assertTrue(message.contains("resourceId=42"));
        assertTrue(message.contains("outcome=SUCCESS"));
        assertFalse(message.contains("generated-password"));
        assertFalse(message.contains("target-user"));
    }

    private <T> T proxy(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new AuditAspect());
        return factory.getProxy();
    }

    private ILoggingEvent onlyEvent() {
        assertEquals(1, appender.list.size());
        return appender.list.getFirst();
    }

    public static class AuditedService {

        @Audited(action = "TEST_UPDATE", resourceType = "account",
                operatorIdExpression = "#operatorId", resourceIdExpression = "#accountId",
                targetIdExpression = "#targetId")
        public String sensitiveOperation(String secret, Integer operatorId, Integer accountId, Integer targetId) {
            return "sensitive-result";
        }

        @Audited(action = "TEST_FAILURE", resourceType = "account",
                operatorIdExpression = "#operatorId", resourceIdExpression = "#accountId")
        public void failingOperation(Integer operatorId, Integer accountId) {
            throw new IllegalStateException("sensitive-error");
        }

        @Audited(action = "TEST_REJECTION", resourceType = "account",
                operatorIdExpression = "#operatorId", resourceIdExpression = "#accountId")
        public RestBean<Void> rejectedOperation(Integer operatorId, Integer accountId) {
            return RestBean.failure(400, "sensitive-message");
        }

        public void ordinaryOperation() {
        }

        public void selfInvokingOperation() {
            sensitiveOperation("self-secret", 1, 2, 3);
        }
    }
}
