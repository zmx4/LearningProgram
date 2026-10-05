package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.request.EmailRegisterVO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验失败必须变成业务响应，而不是被转发到 /error 后伪装成 401 —— 前端会把 401 当作登录过期。
 */
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @SuppressWarnings("unused")
    private void sampleEndpoint(EmailRegisterVO vo) {
    }

    private MethodArgumentNotValidException invalidBody(String field, String message) throws Exception {
        Method method = ApiExceptionHandlerTest.class.getDeclaredMethod("sampleEndpoint", EmailRegisterVO.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new EmailRegisterVO(), "emailRegisterVO");
        bindingResult.rejectValue(field, "Length", message);
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    @Test
    void invalidBodyBecomesBadRequestWithTheFieldMessage() throws Exception {
        RestBean<Void> response = handler.handleInvalidBody(invalidBody("username", "长度需要在1和10之间"));

        assertEquals(400, response.code());
        assertEquals("长度需要在1和10之间", response.message());
    }

    @Test
    void invalidBodyWithoutMessageStillGetsAReadableReason() throws Exception {
        RestBean<Void> response = handler.handleInvalidBody(invalidBody("username", null));

        assertEquals(400, response.code());
        assertTrue(response.message().contains("username"), response.message());
    }

    @Test
    void constraintViolationBecomesBadRequest() {
        EmailRegisterVO vo = new EmailRegisterVO();
        vo.setUsername("tick");
        vo.setEmail("not-an-email");
        vo.setPassword("Str0ng!Pass1");
        vo.setCode("123456");

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<EmailRegisterVO>> violations = validator.validate(vo);
        assertTrue(!violations.isEmpty(), "用例本身应当触发校验失败");

        RestBean<Void> response = handler.handleConstraintViolation(new ConstraintViolationException(violations));

        assertEquals(400, response.code());
        assertTrue(response.message() != null && !response.message().isBlank());
    }

    @Test
    void unmappedPathBecomesNotFoundInsteadOfUnauthorized() {
        NoResourceFoundException notFound =
                new NoResourceFoundException(HttpMethod.GET, "/api/nope", "No static resource");
        assertTrue(notFound instanceof ErrorResponse, "兜底分支依赖 ErrorResponse");

        RestBean<Void> response = handler.handleUnexpected(notFound);

        assertEquals(404, response.code());
        assertEquals("接口不存在", response.message());
    }

    @Test
    void unexpectedFailureBecomesInternalError() {
        RestBean<Void> response = handler.handleUnexpected(new IllegalStateException("boom"));

        assertEquals(500, response.code());
        assertEquals("内部错误,请联系管理员", response.message());
    }

    @Test
    void unreadableBodyBecomesBadRequest() {
        RestBean<Void> response = handler.handleUnreadableBody(
                new HttpMessageNotReadableException("bad json", (HttpInputMessage) null));

        assertEquals(400, response.code());
    }
}
