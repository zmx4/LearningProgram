package com.tick.service;

import com.tick.entity.dto.Cet4Word;
import com.tick.entity.dto.Cet6Word;

import java.util.List;

/**
 * CET 词表服务：随机抽取 CET4 / CET6 单词。
 */
public interface CetWordService {
    /**
     * 随机抽取 count 个 CET4 单词。
     */
    List<Cet4Word> findRandomCet4(int count);

    /**
     * 随机抽取 count 个 CET6 单词。
     */
    List<Cet6Word> findRandomCet6(int count);
}
