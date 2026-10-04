package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.service.AccountPointsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 积分总账查询接口，登录用户可查看自己的总积分。
 */
@RestController
@RequestMapping("/api/points")
public class AccountPointsController {
    private final AccountPointsService accountPointsService;

    public AccountPointsController(AccountPointsService accountPointsService) {
        this.accountPointsService = accountPointsService;
    }

    @GetMapping
    public RestBean<Map<String, Integer>> getTotalPoints(HttpServletRequest request) {
        Integer accountId = (Integer) request.getAttribute("id");
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        return RestBean.success(Map.of("totalPoints", accountPointsService.getTotalPoints(accountId)));
    }
}
