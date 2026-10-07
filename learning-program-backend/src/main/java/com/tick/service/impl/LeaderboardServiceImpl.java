package com.tick.service.impl;

import com.tick.entity.dto.LeaderboardMetric;
import com.tick.entity.vo.response.LeaderboardEntryVO;
import com.tick.entity.vo.response.LeaderboardVO;
import com.tick.mapper.LeaderboardMapper;
import com.tick.service.LeaderboardService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 排行榜服务实现。
 * <p>
 * 三个维度的差别只在「累计值怎么算」，交给 {@link LeaderboardMapper} 的三条查询；
 * 这里统一负责参数归一化、拆出榜单条目与「我的排名」。
 */
@Service
public class LeaderboardServiceImpl implements LeaderboardService {
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;

    private final LeaderboardMapper leaderboardMapper;

    public LeaderboardServiceImpl(LeaderboardMapper leaderboardMapper) {
        this.leaderboardMapper = leaderboardMapper;
    }

    @Override
    public List<Map<String, String>> listMetrics() {
        List<Map<String, String>> metrics = new ArrayList<>();
        for (LeaderboardMetric metric : LeaderboardMetric.values()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", metric.getCode());
            item.put("label", metric.getLabel());
            item.put("unit", metric.getUnit());
            metrics.add(item);
        }
        return metrics;
    }

    @Override
    public LeaderboardVO getLeaderboard(Integer accountId, String metricCode, Integer limit) {
        LeaderboardMetric metric = LeaderboardMetric.fromCode(metricCode);
        int safeLimit = normalizeLimit(limit);

        // 一次查询同时拿到「前 safeLimit 名」和「当前用户那一行」（如果他有记录的话）
        List<LeaderboardEntryVO> rows = switch (metric) {
            case POINTS -> leaderboardMapper.selectPointsRanking(accountId, safeLimit);
            case STUDY -> leaderboardMapper.selectStudyRanking(accountId, safeLimit);
            case CHECK_IN -> leaderboardMapper.selectCheckInRanking(accountId, safeLimit);
        };

        List<LeaderboardEntryVO> entries = rows.stream()
                .filter(row -> row.getRank() != null && row.getRank() <= safeLimit)
                .toList();
        LeaderboardEntryVO me = rows.stream()
                .filter(row -> Objects.equals(row.getAccountId(), accountId))
                .findFirst()
                .orElse(null);
        int rankedUsers = rows.isEmpty() || rows.get(0).getRankedUsers() == null
                ? 0
                : rows.get(0).getRankedUsers();

        return new LeaderboardVO(
                metric.getCode(),
                metric.getLabel(),
                metric.getUnit(),
                safeLimit,
                rankedUsers,
                entries,
                me);
    }

    /** 缺省 20 条；小于 1 按缺省处理，超过上限按上限截断。 */
    static int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
