package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.response.UserProfileVO;
import com.tick.service.AccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户主页接口：按 id 查询用户的公开基本信息（用户名、简介）。
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    private final AccountService accountService;

    public UserController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    public RestBean<UserProfileVO> getUser(@PathVariable Integer id) {
        Account account = accountService.getById(id);
        if (account == null) {
            return RestBean.failure(404, "用户不存在");
        }
        return RestBean.success(UserProfileVO.from(account));
    }
}
