package com.tick.service;

/**
 * 积分总账服务：签到与课程奖励共用同一积分账本。
 */
public interface AccountPointsService {
    /**
     * 查询总积分，无积分记录时返回 0。
     */
    int getTotalPoints(Integer accountId);

    /**
     * 累加积分（Upsert，无记录则建行）。points 必须为正数，否则抛 {@link IllegalArgumentException}。
     */
    void addPoints(Integer accountId, int points);
}
