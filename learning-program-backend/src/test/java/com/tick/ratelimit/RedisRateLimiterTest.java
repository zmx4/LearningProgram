package com.tick.ratelimit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRateLimiterTest {

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void atomicCounterUsesConfiguredWindowAndLimit() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(1L, 3L);
        RedisRateLimiter limiter = new RedisRateLimiter(redis);

        assertTrue(limiter.allow("rate_limit:test", 2, 60));
        assertFalse(limiter.allow("rate_limit:test", 2, 60));

        ArgumentCaptor<String> windowCaptor = ArgumentCaptor.forClass(String.class);
        verify(redis, times(2)).execute(any(RedisScript.class), anyList(), windowCaptor.capture());
        assertEquals(List.of("60", "60"), windowCaptor.getAllValues());
    }
}
