package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.response.LeaderboardVO;
import com.tick.service.LeaderboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 排行榜接口：登录后即可查看。榜单是累计口径（总榜）。
 */
@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /** 可选的排序维度，前端据此渲染切换标签，避免把维度写死在前端。 */
    @GetMapping("/metrics")
    public RestBean<List<Map<String, String>>> metrics() {
        return RestBean.success(leaderboardService.listMetrics());
    }

    /**
     * @param metric 维度，缺省 points；可选值见 /api/leaderboard/metrics
     * @param limit  榜单条数上限，缺省 20，最大 100
     */
    @GetMapping
    public RestBean<LeaderboardVO> leaderboard(
            @RequestParam(required = false, defaultValue = "points") String metric,
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        Integer accountId = (Integer) request.getAttribute("id");
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        try {
            return RestBean.success(leaderboardService.getLeaderboard(accountId, metric, limit));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }
}
