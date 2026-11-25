package com.tick.ratelimit;

import com.tick.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

    @Test
    void requestWithinLimitProceedsAndExcessRequestIsRejected() {
        RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
        when(rateLimiter.allow(anyString(), eq(2), eq(60))).thenReturn(true, true, false);
        RateLimitedService service = proxy(new RateLimitedService(), rateLimiter);

        service.attempt("User@Example.com");
        service.attempt("User@Example.com");
        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> service.attempt("User@Example.com"));

        assertEquals(2, service.calls());
        assertEquals("操作过于频繁，请稍后再试", exception.getMessage());
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(rateLimiter, times(3)).allow(keyCaptor.capture(), eq(2), eq(60));
        List<String> keys = keyCaptor.getAllValues();
        assertTrue(keys.stream().allMatch(key -> key.startsWith("rate_limit:")));
        assertTrue(keys.stream().noneMatch(key -> key.contains("User@Example.com")));
        assertTrue(keys.stream().distinct().count() == 1);
    }

    @Test
    void unannotatedMethodBypassesRateLimiter() {
        RedisRateLimiter rateLimiter = mock(RedisRateLimiter.class);
        RateLimitedService service = proxy(new RateLimitedService(), rateLimiter);

        service.ordinaryOperation();

        assertEquals(1, service.calls());
        verifyNoInteractions(rateLimiter);
    }

    private RateLimitedService proxy(RateLimitedService target, RedisRateLimiter rateLimiter) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new RateLimitAspect(rateLimiter));
        return factory.getProxy();
    }

    public static class RateLimitedService {

        private int calls;

        @RateLimit(limit = 2, windowSeconds = 60, keyExpression = "#email",
                message = "操作过于频繁，请稍后再试")
        public void attempt(String email) {
            calls++;
        }

        public void ordinaryOperation() {
            calls++;
        }

        public int calls() {
            return calls;
        }
    }
}
