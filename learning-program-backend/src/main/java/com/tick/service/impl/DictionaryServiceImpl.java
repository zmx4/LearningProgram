package com.tick.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Dictionary;
import com.tick.mapper.DictionaryMapper;
import com.tick.service.DictionaryService;
import org.springframework.stereotype.Service;

/**
 * 词典查询服务实现。
 */
@Service
public class DictionaryServiceImpl extends ServiceImpl<DictionaryMapper, Dictionary>
        implements DictionaryService {

    @Override
    public Dictionary findByWord(String word) {
        return query()
                .eq("word", word)
                .last("LIMIT 1")
                .one();
    }
}
