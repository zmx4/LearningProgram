package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.WordTestRecord;
import com.tick.service.WordTestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 单词测试接口：提交成绩并查询个人历史。成绩按正确率计分，词表来源见 V8 相关说明。
 */
@RestController
@RequestMapping("/api/tests/words")
public class WordTestController {
    private final WordTestService wordTestService;

    public WordTestController(WordTestService wordTestService) {
        this.wordTestService = wordTestService;
    }

    @GetMapping("/history")
    public RestBean<Map<String, Object>> history(@RequestAttribute("id") Integer accountId) {
        return RestBean.success(Map.of(
                "summary", wordTestService.getSummary(accountId),
                "records", wordTestService.getHistory(accountId)
        ));
    }

    @PostMapping("/results")
    public RestBean<WordTestRecord> save(@RequestBody WordTestRecord record,
                                         @RequestAttribute("id") Integer accountId) {
        return RestBean.success(wordTestService.saveResult(accountId, record));
    }
}
