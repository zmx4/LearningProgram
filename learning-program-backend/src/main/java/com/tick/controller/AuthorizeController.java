package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.request.ConfirmRestVO;
import com.tick.entity.vo.request.EmailRegisterVO;
import com.tick.security.password.PasswordPolicy;
import com.tick.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 注册与密码重置接口。/api/auth/** 在 SecurityConfiguration 中放行匿名访问；
 * spring.security.email 开启后注册与重置需要邮箱验证码，具体校验规则见 AccountServiceImpl。
 * 密码强度要求由 PasswordPolicy 责任链决定，并通过 /password-policy 对外暴露。
 */
@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthorizeController {
    private final AccountService accountService;
    private final PasswordPolicy passwordPolicy;

    public AuthorizeController(AccountService accountService, PasswordPolicy passwordPolicy) {
        this.accountService = accountService;
        this.passwordPolicy = passwordPolicy;
    }

    /**
     * 当前生效的密码强度要求。前端注册 / 重置 / 改密页面据此展示规则并做即时校验，
     * 规则只维护在后端责任链里，不需要前后端各写一份。
     */
    @GetMapping("/password-policy")
    public RestBean<Map<String, Object>> passwordPolicy() {
        return RestBean.success(passwordPolicy.describe());
    }

    @PostMapping("/register")
    public RestBean<Void> register(@RequestBody @Valid EmailRegisterVO vo) {
        return this.messageHandle(vo,accountService::registerEmailAccount);
    }

    @PostMapping("/reset-confirm")
    public RestBean<Void> resetConfirm(@RequestBody @Valid ConfirmRestVO vo) {
        return this.messageHandle(vo, accountService::resetConfirm);
    }

    /**
     * 验证码通过后按邮箱重置密码。
     * 原先这里错接到了 registerEmailAccount，导致对已注册邮箱只会回「此email已被其他用户注册.」，
     * 忘记密码流程实际走不通。
     */
    @PostMapping("/reset-password")
    public RestBean<Void> resetPassword(@RequestBody @Valid EmailRegisterVO vo) {
        return this.messageHandle(vo, accountService::restEmailAccountPassword);
    }

    private <T> RestBean<Void> messageHandle(T vo, Function<T, String> function) {
        return messageHandle(() -> function.apply(vo));
    }

    private RestBean<Void> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
}
