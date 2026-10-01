package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.response.TestQuestionVO;
import com.tick.service.TestQuestionService;
import com.tick.service.TestTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 题目读取接口，登录后即可访问。
 */
@RestController
@RequestMapping("/api/test-questions")
public class TestQuestionController {
    private final TestTypeService testTypeService;
    private final TestQuestionService testQuestionService;

    public TestQuestionController(TestTypeService testTypeService, TestQuestionService testQuestionService) {
        this.testTypeService = testTypeService;
        this.testQuestionService = testQuestionService;
    }

    /**
     * @param typeId 测试类型 id，必填
     * @param kind   题型，可选，取值为 single / multiple / blank
     * @param count  题目数量，可选；不传返回该类型全部题目，传了则随机返回指定数量
     */
    @GetMapping
    public RestBean<List<TestQuestionVO>> questions(
            @RequestParam(required = false) Integer typeId,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) Integer count) {
        if (typeId == null) {
            return RestBean.failure(400, "测试类型不能为空");
        }
        if (testTypeService.getType(typeId) == null) {
            return RestBean.failure(404, "测试类型不存在");
        }
        try {
            return RestBean.success(testQuestionService.getQuestions(typeId, kind, count));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }
}
