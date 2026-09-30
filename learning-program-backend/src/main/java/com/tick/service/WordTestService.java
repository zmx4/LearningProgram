package com.tick.service;

import com.tick.entity.dto.WordTestRecord;

import java.util.List;
import java.util.Map;

public interface WordTestService {
    WordTestRecord saveResult(Integer accountId, WordTestRecord record);
    Map<String, Object> getSummary(Integer accountId);
    List<WordTestRecord> getHistory(Integer accountId);
}
