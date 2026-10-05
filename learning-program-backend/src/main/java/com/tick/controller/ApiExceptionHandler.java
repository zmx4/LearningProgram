package com.tick.controller;

import com.tick.entity.RestBean;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 统一异常处理：把框架抛出的异常转换成项目统一的 {@link RestBean}。
 * <p>
 * 修复的问题：<code>@Valid</code> 校验失败时原本没有处理器，Spring 会转发到 <code>/error</code>，
 * 而该路径又被安全配置要求登录，于是调用方拿到的是
 * <code>401 Full authentication is required to access this resource</code> ——
 * 前端会把任何 401 当作登录过期并清除登录态，导致「用户名太长」这类输入错误直接把用户踢下线。
 * 这里把客户端错误在进入错误分发之前就转成正常的业务响应。
 * <p>
 * 注意：与项目既有约定保持一致，响应体里的 <code>code</code> 才是业务码，HTTP 状态仍是 200。
 * 这样前端 handleResponse 能拿到真正的 message；若返回真实的 4xx，axios 会直接走 catch，
 * 前端反而只能显示「发生了一些错误」。
 */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** {@code @Valid @RequestBody} 校验失败，取第一条错误信息。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RestBean<Void> handleInvalidBody(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(this::messageOf)
                .orElse("请求参数不合法");
        return RestBean.failure(400, message);
    }

    /** {@code @Validated} 作用下方法参数（如 @RequestParam）的约束校验失败。 */
    @ExceptionHandler(ConstraintViolationException.class)
    public RestBean<Void> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("；"));
        return RestBean.failure(400, message.isEmpty() ? "请求参数不合法" : message);
    }

    /** 请求体不是合法 JSON、或字段类型对不上。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public RestBean<Void> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return RestBean.failure(400, "请求体格式不正确");
    }

    /** 缺少必填的查询参数。 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public RestBean<Void> handleMissingParameter(MissingServletRequestParameterException exception) {
        return RestBean.failure(400, "缺少必需的参数：" + exception.getParameterName());
    }

    /** 参数类型不匹配，例如路径变量要求数字却传了字符串。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public RestBean<Void> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return RestBean.failure(400, "参数 " + exception.getName() + " 格式不正确");
    }

    /**
     * 兜底：Spring MVC 的客户端错误异常（找不到接口、方法不支持、媒体类型不支持等）都实现了
     * {@link ErrorResponse}，按它自带的状态码返回；其余才是真正未预期的服务端错误，记日志后统一提示。
     */
    @ExceptionHandler(Exception.class)
    public RestBean<Void> handleUnexpected(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            int status = errorResponse.getStatusCode().value();
            return RestBean.failure(status, messageOfClientError(status));
        }
        log.error("未处理的接口异常", exception);
        return RestBean.failure(500, "内部错误,请联系管理员");
    }

    private String messageOf(ObjectError error) {
        String message = error.getDefaultMessage();
        if (message == null || message.isBlank()) {
            return error instanceof FieldError fieldError
                    ? "参数 " + fieldError.getField() + " 不合法"
                    : "请求参数不合法";
        }
        return message;
    }

    private String messageOfClientError(int status) {
        return switch (status) {
            case 404 -> "接口不存在";
            case 405 -> "请求方法不被支持";
            case 406 -> "无法返回客户端可接受的内容类型";
            case 415 -> "不支持的请求内容类型";
            default -> "请求不合法";
        };
    }
}
