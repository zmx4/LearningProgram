package com.tick.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.WordTestRecord;
import com.tick.mapper.WordTestRecordMapper;
import com.tick.service.WordTestService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WordTestServiceImpl extends ServiceImpl<WordTestRecordMapper, WordTestRecord>
        implements WordTestService {
    @Override
    public WordTestRecord saveResult(Integer accountId, WordTestRecord record) {
        if (!"cet4".equals(record.getSource()) && !"cet6".equals(record.getSource())) {
            throw new IllegalArgumentException("单词来源无效");
        }
        if (record.getTotalCount() == null || record.getTotalCount() < 1
                || record.getCorrectCount() == null
                || record.getCorrectCount() < 0
                || record.getCorrectCount() > record.getTotalCount()) {
            throw new IllegalArgumentException("测试数据无效");
        }
        record.setAccountId(accountId);
        record.setWrongCount(record.getTotalCount() - record.getCorrectCount());
        record.setScore(record.getCorrectCount() * 100 / record.getTotalCount());
        record.setDurationSeconds(Math.max(0, record.getDurationSeconds() == null ? 0 : record.getDurationSeconds()));
        record.setCreatedAt(LocalDateTime.now());
        save(record);
        return record;
    }

    @Override
    public Map<String, Object> getSummary(Integer accountId) {
        List<WordTestRecord> history = getHistory(accountId);
        Map<String, Object> summary = new HashMap<>();
        summary.put("testCount", history.size());
        summary.put("averageScore", history.stream().mapToInt(WordTestRecord::getScore).average().orElse(0));
        summary.put("bestScore", history.stream().mapToInt(WordTestRecord::getScore).max().orElse(0));
        summary.put("totalQuestions", history.stream().mapToInt(WordTestRecord::getTotalCount).sum());
        summary.put("totalCorrect", history.stream().mapToInt(WordTestRecord::getCorrectCount).sum());
        return summary;
    }

    @Override
    public List<WordTestRecord> getHistory(Integer accountId) {
        return query().eq("account_id", accountId).orderByDesc("created_at").list();
    }
}
