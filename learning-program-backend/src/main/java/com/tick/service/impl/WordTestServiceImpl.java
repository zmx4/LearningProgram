package com.tick.service.impl;

import com.tick.entity.dto.TestCategory;
import com.tick.entity.dto.TestRecord;
import com.tick.entity.dto.WordTestRecord;
import com.tick.service.WordTestService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 单词测试服务实现。公共的成绩校验、计分、历史与汇总逻辑见父类 {@link BaseTestService}。
 */
@Service
public class WordTestServiceImpl extends BaseTestService implements WordTestService {
    @Override
    protected TestCategory category() {
        return TestCategory.WORD;
    }

    @Override
    public WordTestRecord saveResult(Integer accountId, WordTestRecord vo) {
        if (!"cet4".equals(vo.getSource()) && !"cet6".equals(vo.getSource())) {
            throw new IllegalArgumentException("单词来源无效");
        }
        validateCounts(vo.getTotalCount(), vo.getCorrectCount(), null);
        TestRecord record = buildRecord(accountId, vo.getTotalCount(), vo.getCorrectCount(),
                vo.getDurationSeconds());
        record.setSource(vo.getSource());
        save(record);
        return toWordRecord(record);
    }

    @Override
    public Map<String, Object> getSummary(Integer accountId) {
        return summarize(history(accountId));
    }

    @Override
    public List<WordTestRecord> getHistory(Integer accountId) {
        return history(accountId).stream().map(this::toWordRecord).toList();
    }

    private WordTestRecord toWordRecord(TestRecord record) {
        WordTestRecord vo = new WordTestRecord();
        copyCommonFields(record, vo);
        vo.setSource(record.getSource());
        return vo;
    }
}
