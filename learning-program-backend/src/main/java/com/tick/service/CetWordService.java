package com.tick.service;

import com.tick.entity.dto.Cet4Word;
import com.tick.entity.dto.Cet6Word;

import java.util.List;

public interface CetWordService {
    List<Cet4Word> findRandomCet4(int count);

    List<Cet6Word> findRandomCet6(int count);
}
