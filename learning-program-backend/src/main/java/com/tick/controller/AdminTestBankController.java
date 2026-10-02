package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.TestType;
import com.tick.entity.vo.request.TestQuestionCreateVO;
import com.tick.entity.vo.request.TestTypeCreateVO;
import com.tick.entity.vo.response.TestQuestionVO;
import com.tick.service.TestQuestionService;
import com.tick.service.TestTypeService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试题库的写入接口。挂在 /api/admin 下，由 SecurityConfiguration 限定为 admin 角色。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminTestBankController {
    private final TestTypeService testTypeService;
    private final TestQuestionService testQuestionService;

    public AdminTestBankController(TestTypeService testTypeService, TestQuestionService testQuestionService) {
        this.testTypeService = testTypeService;
        this.testQuestionService = testQuestionService;
    }

    @PostMapping("/test-types")
    public RestBean<TestType> createType(@RequestBody TestTypeCreateVO vo) {
        try {
            return RestBean.success(testTypeService.createType(vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @PutMapping("/test-types/{id}")
    public RestBean<TestType> updateType(@PathVariable Integer id, @RequestBody TestTypeCreateVO vo) {
        try {
            return RestBean.success(testTypeService.updateType(id, vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @DeleteMapping("/test-types/{id}")
    public RestBean<Void> deleteType(@PathVariable Integer id) {
        try {
            testTypeService.deleteType(id);
            return RestBean.success();
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @PostMapping("/test-questions")
    public RestBean<TestQuestionVO> createQuestion(@RequestBody TestQuestionCreateVO vo) {
        if (vo == null) {
            return RestBean.failure(400, "请求体不能为空");
        }
        if (testTypeService.getType(vo.getTypeId()) == null) {
            return RestBean.failure(400, "测试类型不存在");
        }
        try {
            return RestBean.success(testQuestionService.createQuestion(vo.getTypeId(), vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @PutMapping("/test-questions/{id}")
    public RestBean<TestQuestionVO> updateQuestion(@PathVariable Integer id, @RequestBody TestQuestionCreateVO vo) {
        if (vo == null) {
            return RestBean.failure(400, "请求体不能为空");
        }
        Integer typeId = vo.getTypeId();
        if (typeId == null) {
            return RestBean.failure(400, "测试类型不能为空");
        }
        if (testTypeService.getType(typeId) == null) {
            return RestBean.failure(400, "测试类型不存在");
        }
        try {
            return RestBean.success(testQuestionService.updateQuestion(id, vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @DeleteMapping("/test-questions/{id}")
    public RestBean<Void> deleteQuestion(@PathVariable Integer id) {
        try {
            testQuestionService.deleteQuestion(id);
            return RestBean.success();
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }
}
