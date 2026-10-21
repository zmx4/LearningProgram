package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.ChangePasswordVO;
import com.tick.entity.vo.request.ProfileUpdateVO;
import com.tick.entity.vo.response.ProfileVO;
import com.tick.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public RestBean<ProfileVO> getProfile(@RequestAttribute("id") Integer accountId) {
        Account account = accountService.getById(accountId);
        if (account == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        return RestBean.success(ProfileVO.from(account));
    }

    @PutMapping
    public RestBean<ProfileVO> updateProfile(
            @Valid @RequestBody ProfileUpdateVO vo,
            @RequestAttribute("id") Integer accountId
    ) {
        Account account = accountService.updateProfile(accountId, vo);
        return account == null
                ? RestBean.unauthorized("登录状态无效")
                : RestBean.success(ProfileVO.from(account));
    }

    @PutMapping("/password")
    public RestBean<Void> changePassword(
            @Valid @RequestBody ChangePasswordVO vo,
            @RequestAttribute("id") Integer accountId
    ) {
        String message = accountService.changePassword(accountId, vo);
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }
}
