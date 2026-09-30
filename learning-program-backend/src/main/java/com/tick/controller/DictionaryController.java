package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.Dictionary;
import com.tick.service.DictionaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dictionary")
public class DictionaryController {
    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    @GetMapping
    public RestBean<Dictionary> findByQuery(@RequestParam(required = false) String word) {
        return find(word);
    }

    @GetMapping("/{word}")
    public RestBean<Dictionary> findByPath(@PathVariable String word) {
        return find(word);
    }

    private RestBean<Dictionary> find(String word) {
        if (word == null || word.isBlank() || word.length() > 255) {
            return RestBean.failure(400, "单词参数不能为空且长度不能超过255个字符");
        }

        Dictionary dictionary = dictionaryService.findByWord(word.trim());
        if (dictionary == null) {
            return RestBean.failure(404, "单词不存在");
        }
        return RestBean.success(dictionary);
    }
}
