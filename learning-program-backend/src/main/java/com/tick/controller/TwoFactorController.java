package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.TotpCodeVO;
import com.tick.entity.vo.request.TotpSetupVO;
import com.tick.service.AccountService;
import com.tick.service.TwoFactorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public RestBean<Map<String, Object>> status(@RequestAttribute("id") Integer accountId) {
        Account account = accountService.getById(accountId);
        if (account == null) return RestBean.unauthorized("登录状态无效");
        return RestBean.success(Map.of(
                "enabled", twoFactorService.isEnabled(account),
                "emailVerificationRequired", emailVerificationEnabled,
                "email", account.getEmail() == null ? "" : account.getEmail()
        ));
    }

    @PostMapping("/setup")
    public RestBean<com.tick.entity.vo.response.TotpSetupVO> setup(@Valid @RequestBody TotpSetupVO vo,
                                                                   @RequestAttribute("id") Integer accountId) {
        return RestBean.success(twoFactorService.setup(accountId, vo.getEmailCode()));
    }

    @PostMapping("/enable")
    public RestBean<Void> enable(@Valid @RequestBody TotpCodeVO vo,
                                 @RequestAttribute("id") Integer accountId) {
        String error = twoFactorService.enable(accountId, vo.getCode());
        return error == null ? RestBean.success() : RestBean.failure(400, error);
    }

    @DeleteMapping
    public RestBean<Void> disable(@RequestAttribute("id") Integer accountId) {
        String error = twoFactorService.disable(accountId);
        return error == null ? RestBean.success() : RestBean.failure(400, error);
    }
}
