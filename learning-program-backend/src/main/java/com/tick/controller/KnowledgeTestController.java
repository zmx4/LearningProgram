package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.request.KnowledgeTestResultVO;
import com.tick.entity.vo.response.KnowledgeTestRecordVO;
import com.tick.service.KnowledgeTestService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tests/knowledge")
public class KnowledgeTestController {
    private final KnowledgeTestService knowledgeTestService;

    public KnowledgeTestController(KnowledgeTestService knowledgeTestService) {
        this.knowledgeTestService = knowledgeTestService;
    }

    @GetMapping("/history")
    public RestBean<Map<String, Object>> history(HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) return RestBean.unauthorized("登录状态无效");
        return RestBean.success(Map.of(
                "summary", knowledgeTestService.getSummary(accountId),
                "records", knowledgeTestService.getHistory(accountId)
        ));
    }

    @PostMapping("/results")
    public RestBean<KnowledgeTestRecordVO> save(@RequestBody KnowledgeTestResultVO vo, HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) return RestBean.unauthorized("登录状态无效");
        try {
            return RestBean.success(knowledgeTestService.saveResult(accountId, vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    private Integer accountId(HttpServletRequest request) {
        return (Integer) request.getAttribute("id");
    }
}
