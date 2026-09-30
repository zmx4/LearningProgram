package com.tick.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.tick.entity.dto.Dictionary;

public interface DictionaryService extends IService<Dictionary> {
    Dictionary findByWord(String word);
}
