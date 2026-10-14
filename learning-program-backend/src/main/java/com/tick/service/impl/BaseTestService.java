package com.tick.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.BaseTestRecord;
import com.tick.entity.dto.TestCategory;
import com.tick.entity.dto.TestRecord;
import com.tick.mapper.TestRecordMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试成绩服务父类：单词测试与知识测试共用计数校验、按正确率计分、历史查询与汇总逻辑。
 * 子类只需声明自己的测试大类（{@link #category()}）并补充各自特有的字段与校验。
 */
public abstract class BaseTestService extends ServiceImpl<TestRecordMapper, TestRecord> {

    /**
     * 当前服务处理的测试大类，入库与查询均以此区分。
     */
    protected abstract TestCategory category();

    /**
     * 校验题目数量与正确数。maxTotalCount 为 null 时不限制题目数量上限。
     */
    protected void validateCounts(Integer totalCount, Integer correctCount, Integer maxTotalCount) {
        if (totalCount == null || totalCount < 1
                || (maxTotalCount != null && totalCount > maxTotalCount)) {
            throw new IllegalArgumentException(maxTotalCount == null
                    ? "测试数据无效"
                    : "题目数量必须在 1 到 " + maxTotalCount + " 之间");
        }
        if (correctCount == null || correctCount < 0 || correctCount > totalCount) {
            throw new IllegalArgumentException("测试数据无效");
        }
    }

    /**
     * 构造一条待保存的成绩记录：错误数由总数减正确数得出，得分按正确率计算。
     */
    protected TestRecord buildRecord(Integer accountId, Integer totalCount, Integer correctCount,
                                     Integer durationSeconds) {
        TestRecord record = new TestRecord();
        record.setCategory(category().getCode());
        record.setAccountId(accountId);
        record.setTotalCount(totalCount);
        record.setCorrectCount(correctCount);
        record.setWrongCount(totalCount - correctCount);
        record.setScore(correctCount * 100 / totalCount);
        record.setDurationSeconds(Math.max(0, durationSeconds == null ? 0 : durationSeconds));
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }

    /**
     * 当前测试大类的历史成绩记录，按时间倒序。
     */
    protected List<TestRecord> history(Integer accountId) {
        return query().eq("account_id", accountId)
                .eq("category", category().getCode())
                .orderByDesc("created_at")
                .list();
    }

    /**
     * 汇总统计：累计测试次数、平均分、最高分、累计题目数与累计正确数。
     */
    protected Map<String, Object> summarize(List<? extends BaseTestRecord> history) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("testCount", history.size());
        summary.put("averageScore", history.stream().mapToInt(BaseTestRecord::getScore).average().orElse(0));
        summary.put("bestScore", history.stream().mapToInt(BaseTestRecord::getScore).max().orElse(0));
        summary.put("totalQuestions", history.stream().mapToInt(BaseTestRecord::getTotalCount).sum());
        summary.put("totalCorrect", history.stream().mapToInt(BaseTestRecord::getCorrectCount).sum());
        return summary;
    }

    /**
     * 把统一记录中的公共字段拷贝到指定测试大类的接口对象上。
     */
    protected void copyCommonFields(TestRecord record, BaseTestRecord target) {
        target.setId(record.getId());
        target.setAccountId(record.getAccountId());
        target.setTotalCount(record.getTotalCount());
        target.setCorrectCount(record.getCorrectCount());
        target.setWrongCount(record.getWrongCount());
        target.setScore(record.getScore());
        target.setDurationSeconds(record.getDurationSeconds());
        target.setCreatedAt(record.getCreatedAt());
    }
}
