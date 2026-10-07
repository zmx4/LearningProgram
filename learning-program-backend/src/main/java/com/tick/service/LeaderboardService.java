package com.tick.service;

import com.tick.entity.vo.response.LeaderboardVO;

import java.util.List;
import java.util.Map;

/**
 * 排行榜服务：按维度返回前若干名，并附上当前用户自己的名次。
 */
public interface LeaderboardService {
    /** 可选维度列表（供前端渲染切换标签），元素含 code / label / unit。 */
    List<Map<String, String>> listMetrics();

    /**
     * 查询榜单。
     *
     * @param accountId  当前用户，用于返回「我的排名」
     * @param metricCode 维度标识，取值见 {@link com.tick.entity.dto.LeaderboardMetric}
     * @param limit      榜单条数上限，缺省 20，最大 100
     * @throws IllegalArgumentException 维度不支持时抛出，message 可直接展示
     */
    LeaderboardVO getLeaderboard(Integer accountId, String metricCode, Integer limit);
}
