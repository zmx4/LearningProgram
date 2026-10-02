package com.tick.service;

import com.tick.entity.vo.request.TestQuestionCreateVO;
import com.tick.entity.vo.response.TestQuestionVO;

import java.util.List;

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

    TestQuestionVO createQuestion(Integer typeId, TestQuestionCreateVO vo);

    TestQuestionVO updateQuestion(Integer id, TestQuestionCreateVO vo);

    void deleteQuestion(Integer id);
}
