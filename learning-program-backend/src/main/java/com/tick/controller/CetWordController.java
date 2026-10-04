package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Cet4Word;
import com.tick.entity.dto.Cet6Word;
import com.tick.service.CetWordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CET 词表接口，登录用户可随机拉取 CET4 / CET6 单词，供每日一词与单词测试使用。
 */
@RestController
@RequestMapping("/api/dictionary")
public class CetWordController {
    private static final int DEFAULT_COUNT = 1;
    private static final int MAX_COUNT = 100;

    private final CetWordService cetWordService;

    public CetWordController(CetWordService cetWordService) {
        this.cetWordService = cetWordService;
    }

    @GetMapping("/cet4")
    public RestBean<List<Cet4Word>> cet4(
            @RequestParam(defaultValue = "1") int count) {
        if (!isValidCount(count)) {
            return RestBean.failure(400, "数量必须在1到100之间");
        }
        return RestBean.success(cetWordService.findRandomCet4(count));
    }

    @GetMapping("/cet6")
    public RestBean<List<Cet6Word>> cet6(
            @RequestParam(defaultValue = "1") int count) {
        if (!isValidCount(count)) {
            return RestBean.failure(400, "数量必须在1到100之间");
        }
        return RestBean.success(cetWordService.findRandomCet6(count));
    }

    private boolean isValidCount(int count) {
        return count >= DEFAULT_COUNT && count <= MAX_COUNT;
    }
}
