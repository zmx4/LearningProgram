package com.tick.service.impl;

import com.tick.entity.dto.Cet4Word;
import com.tick.entity.dto.Cet6Word;
import com.tick.mapper.Cet4WordMapper;
import com.tick.mapper.Cet6WordMapper;
import com.tick.service.CetWordService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CET 词表服务实现。
 */
@Service
public class CetWordServiceImpl implements CetWordService {
    private final Cet4WordMapper cet4WordMapper;
    private final Cet6WordMapper cet6WordMapper;

    public CetWordServiceImpl(Cet4WordMapper cet4WordMapper, Cet6WordMapper cet6WordMapper) {
        this.cet4WordMapper = cet4WordMapper;
        this.cet6WordMapper = cet6WordMapper;
    }

    @Override
    public List<Cet4Word> findRandomCet4(int count) {
        return cet4WordMapper.selectRandom(count);
    }

    @Override
    public List<Cet6Word> findRandomCet6(int count) {
        return cet6WordMapper.selectRandom(count);
    }
}
