package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.response.QuestionSetDetailVO;
import com.tick.entity.vo.response.QuestionSetVO;
import com.tick.service.QuestionSetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 题集读取接口，登录后即可访问。
 */
@RestController
@RequestMapping("/api/question-sets")
public class QuestionSetController {
    private final QuestionSetService questionSetService;

    public QuestionSetController(QuestionSetService questionSetService) {
        this.questionSetService = questionSetService;
    }

    @GetMapping
    public RestBean<List<QuestionSetVO>> sets() {
        return RestBean.success(questionSetService.listSets());
    }

    @GetMapping("/{id}")
    public RestBean<QuestionSetDetailVO> setDetail(@PathVariable Integer id) {
        QuestionSetDetailVO detail = questionSetService.getSetDetail(id);
        if (detail == null) {
            return RestBean.failure(404, "题集不存在");
        }
        return RestBean.success(detail);
    }
}
