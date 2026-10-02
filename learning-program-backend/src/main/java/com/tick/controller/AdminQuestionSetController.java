package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.request.QuestionSetSaveVO;
import com.tick.entity.vo.response.QuestionSetVO;
import com.tick.service.QuestionSetService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题集的写入接口。挂在 /api/admin 下，由 SecurityConfiguration 限定为 admin 角色。
 */
@RestController
@RequestMapping("/api/admin/question-sets")
public class AdminQuestionSetController {
    private final QuestionSetService questionSetService;

    public AdminQuestionSetController(QuestionSetService questionSetService) {
        this.questionSetService = questionSetService;
    }

    @PostMapping
    public RestBean<QuestionSetVO> createSet(@RequestBody QuestionSetSaveVO vo) {
        try {
            return RestBean.success(questionSetService.createSet(vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @PutMapping("/{id}")
    public RestBean<QuestionSetVO> updateSet(@PathVariable Integer id, @RequestBody QuestionSetSaveVO vo) {
        try {
            return RestBean.success(questionSetService.updateSet(id, vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public RestBean<Void> deleteSet(@PathVariable Integer id) {
        try {
            questionSetService.deleteSet(id);
            return RestBean.success();
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }
}
