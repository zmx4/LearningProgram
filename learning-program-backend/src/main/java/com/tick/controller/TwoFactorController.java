package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.TotpCodeVO;
import com.tick.entity.vo.request.TotpSetupVO;
import com.tick.service.AccountService;
import com.tick.service.TwoFactorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile/2fa")
public class TwoFactorController {
    private final TwoFactorService twoFactorService;
    private final AccountService accountService;
    private final boolean emailVerificationEnabled;

    public TwoFactorController(TwoFactorService twoFactorService, AccountService accountService,
                               @Value("${spring.security.email.enable}") boolean emailVerificationEnabled) {
        this.twoFactorService = twoFactorService;
        this.accountService = accountService;
        this.emailVerificationEnabled = emailVerificationEnabled;
    }

    @GetMapping
    public RestBean<Map<String, Object>> status(HttpServletRequest request) {
        Account account = account(request);
        if (account == null) return RestBean.unauthorized("登录状态无效");
        return RestBean.success(Map.of(
                "enabled", twoFactorService.isEnabled(account),
                "emailVerificationRequired", emailVerificationEnabled,
                "email", account.getEmail() == null ? "" : account.getEmail()
        ));
    }

    @PostMapping("/setup")
    public RestBean<com.tick.entity.vo.response.TotpSetupVO> setup(@Valid @RequestBody TotpSetupVO vo, HttpServletRequest request) {
        Integer id = id(request);
        if (id == null) return RestBean.unauthorized("登录状态无效");
        try {
            return RestBean.success(twoFactorService.setup(id, vo.getEmailCode()));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @PostMapping("/enable")
    public RestBean<Void> enable(@Valid @RequestBody TotpCodeVO vo, HttpServletRequest request) {
        Integer id = id(request);
        if (id == null) return RestBean.unauthorized("登录状态无效");
        String error = twoFactorService.enable(id, vo.getCode());
        return error == null ? RestBean.success() : RestBean.failure(400, error);
    }

    @DeleteMapping
    public RestBean<Void> disable(HttpServletRequest request) {
        Integer id = id(request);
        if (id == null) return RestBean.unauthorized("登录状态无效");
        String error = twoFactorService.disable(id);
        return error == null ? RestBean.success() : RestBean.failure(400, error);
    }

    private Integer id(HttpServletRequest request) {
        return (Integer) request.getAttribute("id");
    }

    private Account account(HttpServletRequest request) {
        Integer id = id(request);
        return id == null ? null : twoFactorServiceAccount(id);
    }

    private Account twoFactorServiceAccount(Integer id) {
        return accountService.getById(id);
    }
}
