package com.tick.monitor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceMonitorAspectTest {

    private final Logger monitorLogger = (Logger) LoggerFactory.getLogger(PerformanceMonitorAspect.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level originalLevel;

    @BeforeEach
    void capturePerformanceLogs() {
        originalLevel = monitorLogger.getLevel();
        monitorLogger.setLevel(Level.INFO);
        appender.start();
        monitorLogger.addAppender(appender);
    }

    @AfterEach
    void releasePerformanceLogs() {
        monitorLogger.detachAppender(appender);
        appender.stop();
        monitorLogger.setLevel(originalLevel);
    }

    @Test
    void slowMethodLogsDurationWithoutSensitiveValues() {
        MonitoredService proxy = proxy(new MonitoredService());

        String result = proxy.slowOperation("secret-argument");

        assertEquals("sensitive-result", result);
        ILoggingEvent event = onlyEvent();
        String message = event.getFormattedMessage();
        assertEquals("PERFORMANCE", event.getMarker().getName());
        assertTrue(message.contains("method=MonitoredService.slowOperation"));
        assertTrue(message.contains("thresholdMs=0"));
        assertTrue(message.contains("outcome=SUCCESS"));
        assertFalse(message.contains("secret-argument"));
        assertFalse(message.contains("sensitive-result"));
    }

    @Test
    void fastMethodDoesNotProduceSlowLog() {
        proxy(new MonitoredService()).fastOperation();

        assertTrue(appender.list.isEmpty());
    }

    @Test
    void slowFailureIsLoggedAndRethrown() {
        MonitoredService proxy = proxy(new MonitoredService());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                proxy::failingOperation);

        assertEquals("sensitive-error", exception.getMessage());
        String message = onlyEvent().getFormattedMessage();
        assertTrue(message.contains("outcome=FAILED"));
        assertFalse(message.contains("sensitive-error"));
    }

    @Test
    void unannotatedMethodIsNotMonitored() {
        proxy(new MonitoredService()).ordinaryOperation();

        assertTrue(appender.list.isEmpty());
    }

    private <T> T proxy(T target) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new PerformanceMonitorAspect());
        return factory.getProxy();
    }

    private ILoggingEvent onlyEvent() {
        assertEquals(1, appender.list.size());
        return appender.list.getFirst();
    }

    public static class MonitoredService {

        @Monitored(slowThresholdMillis = 0)
        public String slowOperation(String secret) {
            return "sensitive-result";
        }

        @Monitored(slowThresholdMillis = Long.MAX_VALUE)
        public void fastOperation() {
        }

        @Monitored(slowThresholdMillis = 0)
        public void failingOperation() {
            throw new IllegalStateException("sensitive-error");
        }

        public void ordinaryOperation() {
        }
    }
}
