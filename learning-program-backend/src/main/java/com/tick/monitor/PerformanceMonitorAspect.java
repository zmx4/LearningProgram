package com.tick.monitor;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PerformanceMonitorAspect {

    private static final Marker PERFORMANCE = MarkerFactory.getMarker("PERFORMANCE");

    @Around("@annotation(monitored)")
    public Object monitor(ProceedingJoinPoint joinPoint, Monitored monitored) throws Throwable {
        long startedAt = System.nanoTime();
        Object result = null;
        Throwable failure = null;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
            if (durationMillis >= monitored.slowThresholdMillis()) {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                log.warn(PERFORMANCE, "method={}.{} durationMs={} thresholdMs={} outcome={}",
                        joinPoint.getTarget().getClass().getSimpleName(), signature.getName(),
                        durationMillis, monitored.slowThresholdMillis(),
                        failure == null ? "SUCCESS" : "FAILED");
            }
        }
        return result;
    }
}
