package com.tick.ratelimit;

import com.tick.exception.TooManyRequestsException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitAspect {

    private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();
    private static final String KEY_PREFIX = "rate_limit:";

    private final RedisRateLimiter rateLimiter;

    public RateLimitAspect(RedisRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Around("@annotation(com.tick.ratelimit.RateLimit)")
    public Object applyRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RateLimit rule = signature.getMethod().getAnnotation(RateLimit.class);
        Object dimension = evaluate(rule.keyExpression(), joinPoint);
        String key = buildKey(joinPoint, dimension);
        if (!rateLimiter.allow(key, rule.limit(), rule.windowSeconds())) {
            throw new TooManyRequestsException(rule.message());
        }
        return joinPoint.proceed();
    }

    private Object evaluate(String expression, ProceedingJoinPoint joinPoint) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = signature.getParameterNames();
        Object[] arguments = joinPoint.getArgs();
        for (int index = 0; index < arguments.length; index++) {
            context.setVariable(parameterNames[index], arguments[index]);
        }
        Expression parsed = EXPRESSION_PARSER.parseExpression(expression);
        return parsed.getValue(context);
    }

    private String buildKey(ProceedingJoinPoint joinPoint, Object dimension) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String normalizedDimension = dimension == null
                ? "" : dimension.toString().trim().toLowerCase(Locale.ROOT);
        String identity = signature.getDeclaringTypeName() + "#" + signature.getName()
                + ":" + normalizedDimension;
        return KEY_PREFIX + sha256(identity);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
