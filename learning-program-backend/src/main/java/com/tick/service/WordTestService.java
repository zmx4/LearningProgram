package com.tick.service;

import com.tick.entity.dto.WordTestRecord;

import java.util.List;
import java.util.Map;

/**
 * 单词测试服务：成绩保存（按正确率计分）、汇总与历史查询。
 */
public interface WordTestService {
    /**
     * 保存一次单词测试成绩：得分按正确数百分比计算。
     */
    WordTestRecord saveResult(Integer accountId, WordTestRecord record);

    /**
     * 汇总统计：累计测试次数、平均分等。
     */
    Map<String, Object> getSummary(Integer accountId);

    /**
     * 历史成绩记录，按时间倒序。
     */
    List<WordTestRecord> getHistory(Integer accountId);
}
