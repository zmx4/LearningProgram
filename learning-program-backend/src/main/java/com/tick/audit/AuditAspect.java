package com.tick.audit;

import com.tick.entity.RestBean;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Aspect
@Component
public class AuditAspect {

    private static final Marker AUDIT = MarkerFactory.getMarker("AUDIT");
    private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        long startedAt = System.nanoTime();
        Object result = null;
        Throwable failure = null;
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            Object operatorId = evaluate(audited.operatorIdExpression(), joinPoint);
            Object resourceId = evaluate(audited.resourceIdExpression(), joinPoint);
            Object targetId = evaluate(audited.targetIdExpression(), joinPoint);
            long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
            String outcome = failure != null ? "FAILED" : isSuccess(result) ? "SUCCESS" : "REJECTED";
            String errorType = failure == null ? "-" : failure.getClass().getSimpleName();

            // 审计只记录标识与结果，避免密码、验证码等参数或返回值进入日志。
            if (failure == null && "SUCCESS".equals(outcome)) {
                log.info(AUDIT,
                        "action={} operatorId={} operator={} resourceType={} resourceId={} targetId={} outcome={} durationMs={} errorType={}",
                        audited.action(), operatorId, operatorName(), audited.resourceType(),
                        resourceId, targetId, outcome, durationMillis, errorType);
            } else {
                log.warn(AUDIT,
                        "action={} operatorId={} operator={} resourceType={} resourceId={} targetId={} outcome={} durationMs={} errorType={}",
                        audited.action(), operatorId, operatorName(), audited.resourceType(),
                        resourceId, targetId, outcome, durationMillis, errorType);
            }
        }
        return result;
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

    private String operatorName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        }
        return "anonymous";
    }

    private boolean isSuccess(Object result) {
        return !(result instanceof RestBean<?> response) || response.code() == 200;
    }
}
