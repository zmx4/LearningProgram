package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Account;
import com.tick.service.AccountService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Resource
    private AccountService accountService;

    @GetMapping("/{id}")
    public RestBean<Map<String, String>> getUser(@PathVariable Integer id) {
        Account account = accountService.getById(id);
        if (account == null) {
            return RestBean.failure(404, "用户不存在");
        }

        return RestBean.success(Map.of(
                "username", account.getUsername(),
                "email", account.getEmail() == null ? "" : account.getEmail(),
                "bio", account.getBio() == null ? "" : account.getBio(),
                "avatar", "temporary"
        ));
    }
}
