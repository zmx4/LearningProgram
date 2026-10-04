package com.tick.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tick.entity.dto.Dictionary;

/**
 * 词典查询服务。
 */
public interface DictionaryService extends IService<Dictionary> {
    /**
     * 按单词查询释义，未收录返回 null。
     */
    Dictionary findByWord(String word);
}
