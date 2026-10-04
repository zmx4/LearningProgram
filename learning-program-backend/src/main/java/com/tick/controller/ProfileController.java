package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.ChangePasswordVO;
import com.tick.entity.vo.request.ProfileUpdateVO;
import com.tick.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 个人资料接口：当前登录用户的资料查看与更新、修改密码。
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final AccountService accountService;

    public ProfileController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public RestBean<Map<String, String>> getProfile(HttpServletRequest request) {
        Account account = findAccount(request);
        if (account == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        return RestBean.success(profile(account));
    }

    @PutMapping
    public RestBean<Map<String, String>> updateProfile(
            @Valid @RequestBody ProfileUpdateVO vo,
            HttpServletRequest request
    ) {
        Integer accountId = (Integer) request.getAttribute("id");
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        try {
            Account account = accountService.updateProfile(accountId, vo);
            return account == null
                    ? RestBean.unauthorized("登录状态无效")
                    : RestBean.success(profile(account));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(409, exception.getMessage());
        }
    }

    @PutMapping("/password")
    public RestBean<Void> changePassword(
            @Valid @RequestBody ChangePasswordVO vo,
            HttpServletRequest request
    ) {
        Integer accountId = (Integer) request.getAttribute("id");
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        String message = accountService.changePassword(accountId, vo);
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }

    private Account findAccount(HttpServletRequest request) {
        Integer accountId = (Integer) request.getAttribute("id");
        return accountId == null ? null : accountService.getById(accountId);
    }

    private Map<String, String> profile(Account account) {
        return Map.of(
                "username", account.getUsername(),
                "email", account.getEmail() == null ? "" : account.getEmail(),
                "phone", account.getPhone() == null ? "" : account.getPhone(),
                "bio", account.getBio() == null ? "" : account.getBio(),
                "role", account.getRole() == null ? "user" : account.getRole()
        );
    }
}
