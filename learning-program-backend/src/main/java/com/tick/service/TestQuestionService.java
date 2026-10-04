package com.tick.service;

import com.tick.entity.vo.request.TestQuestionCreateVO;
import com.tick.entity.vo.response.TestQuestionVO;

import java.util.List;

/**
 * 题库题目服务：按类型/题型取题（支持随机抽取）与管理端维护。
 */
public interface TestQuestionService {
    /**
     * 获取某个测试类型下的题目。count 为 null 时返回全部题目（按 id 升序），
     * 否则随机返回指定数量；kind 为 null 时不限题型。
     */
    List<TestQuestionVO> getQuestions(Integer typeId, String kind, Integer count);

    /**
     * 按 id 批量获取题目，返回顺序与传入的 ids 顺序一致。
     */
    List<TestQuestionVO> listByIds(List<Integer> ids);

    /**
     * 新建题目：校验类型存在与选项/答案结构合法。
     */
    TestQuestionVO createQuestion(Integer typeId, TestQuestionCreateVO vo);

    /**
     * 更新题目内容，题集引用不受影响。
     */
    TestQuestionVO updateQuestion(Integer id, TestQuestionCreateVO vo);

    /**
     * 删除题目；被题集引用时抛 {@link IllegalArgumentException}，需先从题集移除。
     */
    void deleteQuestion(Integer id);
}
