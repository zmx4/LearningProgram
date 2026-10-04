package com.tick.service;

import com.tick.entity.vo.request.KnowledgeTestResultVO;
import com.tick.entity.vo.response.KnowledgeTestRecordVO;

import java.util.List;
import java.util.Map;

/**
 * 知识测试服务：成绩保存（按正确率计分）、汇总与历史查询。
 */
public interface KnowledgeTestService {
    /**
     * 保存一次测试成绩：得分按正确数百分比计算，答题明细经清洗（去脏项、截断过长答案）后入库。
     */
    KnowledgeTestRecordVO saveResult(Integer accountId, KnowledgeTestResultVO vo);

    /**
     * 汇总统计：累计测试次数、平均分等。
     */
    Map<String, Object> getSummary(Integer accountId);

    /**
     * 历史成绩记录，按时间倒序。
     */
    List<KnowledgeTestRecordVO> getHistory(Integer accountId);
}
