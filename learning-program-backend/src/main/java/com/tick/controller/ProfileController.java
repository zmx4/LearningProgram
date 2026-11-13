package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.ChangePasswordVO;
import com.tick.entity.vo.request.ProfileUpdateVO;
import com.tick.entity.vo.response.ProfileVO;
import com.tick.service.AccountService;
import com.tick.service.AvatarService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人资料接口：当前登录用户的资料查看与更新、修改密码、头像上传与移除。
 * 头像仅在启用对象存储时可用。
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final AccountService accountService;
    private final AvatarService avatarService;

    public ProfileController(AccountService accountService, AvatarService avatarService) {
        this.accountService = accountService;
        this.avatarService = avatarService;
    }

    @GetMapping
    public RestBean<ProfileVO> getProfile(@RequestAttribute("id") Integer accountId) {
        Account account = accountService.getById(accountId);
        if (account == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        return RestBean.success(ProfileVO.from(account, avatarService.enabled()));
    }

    @PutMapping
    public RestBean<ProfileVO> updateProfile(
            @Valid @RequestBody ProfileUpdateVO vo,
            @RequestAttribute("id") Integer accountId
    ) {
        Account account = accountService.updateProfile(accountId, vo);
        return account == null
                ? RestBean.unauthorized("登录状态无效")
                : RestBean.success(ProfileVO.from(account, avatarService.enabled()));
    }

    @PutMapping("/password")
    public RestBean<Void> changePassword(
            @Valid @RequestBody ChangePasswordVO vo,
            @RequestAttribute("id") Integer accountId
    ) {
        String message = accountService.changePassword(accountId, vo);
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }

    @PostMapping("/avatar")
    public RestBean<ProfileVO> uploadAvatar(@RequestParam("file") MultipartFile file,
                                            @RequestAttribute("id") Integer accountId) {
        Account account = avatarService.upload(accountId, file);
        return account == null
                ? RestBean.unauthorized("登录状态无效")
                : RestBean.success(ProfileVO.from(account, avatarService.enabled()));
    }

    @DeleteMapping("/avatar")
    public RestBean<ProfileVO> removeAvatar(@RequestAttribute("id") Integer accountId) {
        Account account = avatarService.remove(accountId);
        return account == null
                ? RestBean.unauthorized("登录状态无效")
                : RestBean.success(ProfileVO.from(account, avatarService.enabled()));
    }
}