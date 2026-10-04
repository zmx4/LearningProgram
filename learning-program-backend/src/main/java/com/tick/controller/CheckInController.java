package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.CheckInRecord;
import com.tick.service.CheckInService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Map;

/**
 * 每日签到接口：按月查询签到日历、连签与积分状态，签到按连签天数发放积分。
 */
@RestController
@RequestMapping("/api/check-in")
public class CheckInController {
    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @GetMapping
    public RestBean<Map<String, Object>> status(@RequestParam int year, @RequestParam int month,
                                                 HttpServletRequest request) {
        Integer accountId = (Integer) request.getAttribute("id");
        if (accountId == null) return RestBean.unauthorized("登录状态无效");
        try {
            LocalDate.of(year, month, 1);
            return RestBean.success(checkInService.getStatus(accountId, year, month));
        } catch (DateTimeException exception) {
            return RestBean.failure(400, "日期参数无效");
        }
    }

    @PostMapping
    public RestBean<CheckInRecord> checkIn(HttpServletRequest request) {
        Integer accountId = (Integer) request.getAttribute("id");
        if (accountId == null) return RestBean.unauthorized("登录状态无效");
        return RestBean.success(checkInService.checkIn(accountId));
    }
}
