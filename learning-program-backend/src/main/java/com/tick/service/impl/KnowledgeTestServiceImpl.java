package com.tick.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.KnowledgeTestAnswer;
import com.tick.entity.dto.KnowledgeTestRecord;
import com.tick.entity.dto.TestQuestionKind;
import com.tick.entity.dto.TestType;
import com.tick.entity.vo.request.KnowledgeTestResultVO;
import com.tick.entity.vo.response.KnowledgeTestRecordVO;
import com.tick.mapper.KnowledgeTestRecordMapper;
import com.tick.service.KnowledgeTestService;
import com.tick.service.TestTypeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeTestServiceImpl extends ServiceImpl<KnowledgeTestRecordMapper, KnowledgeTestRecord>
        implements KnowledgeTestService {
    private static final int MAX_TOTAL_COUNT = 100;
    private static final int MAX_ANSWER_LENGTH = 200;
    private static final int MAX_ANSWER_ITEMS = 10;

    private final TestTypeService testTypeService;

    public KnowledgeTestServiceImpl(TestTypeService testTypeService) {
        this.testTypeService = testTypeService;
    }

    @Override
    public KnowledgeTestRecordVO saveResult(Integer accountId, KnowledgeTestResultVO vo) {
        if (vo == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        TestType type = testTypeService.getType(vo.getTypeId());
        if (type == null) {
            throw new IllegalArgumentException("测试类型不存在");
        }
        if (vo.getTotalCount() == null || vo.getTotalCount() < 1 || vo.getTotalCount() > MAX_TOTAL_COUNT) {
            throw new IllegalArgumentException("题目数量必须在 1 到 " + MAX_TOTAL_COUNT + " 之间");
        }
        if (vo.getCorrectCount() == null
                || vo.getCorrectCount() < 0
                || vo.getCorrectCount() > vo.getTotalCount()) {
            throw new IllegalArgumentException("测试数据无效");
        }

        KnowledgeTestRecord record = new KnowledgeTestRecord();
        record.setAccountId(accountId);
        record.setTypeId(type.getId());
        record.setTypeName(type.getName());
        record.setTotalCount(vo.getTotalCount());
        record.setCorrectCount(vo.getCorrectCount());
        record.setWrongCount(vo.getTotalCount() - vo.getCorrectCount());
        record.setScore(vo.getCorrectCount() * 100 / vo.getTotalCount());
        record.setDurationSeconds(Math.max(0, vo.getDurationSeconds() == null ? 0 : vo.getDurationSeconds()));
        record.setDetail(toDetailJson(vo.getDetail()));
        record.setCreatedAt(LocalDateTime.now());
        save(record);
        return toVO(record);
    }

    @Override
    public Map<String, Object> getSummary(Integer accountId) {
        List<KnowledgeTestRecordVO> history = getHistory(accountId);
        Map<String, Object> summary = new HashMap<>();
        summary.put("testCount", history.size());
        summary.put("averageScore", history.stream().mapToInt(KnowledgeTestRecordVO::getScore).average().orElse(0));
        summary.put("bestScore", history.stream().mapToInt(KnowledgeTestRecordVO::getScore).max().orElse(0));
        summary.put("totalQuestions", history.stream().mapToInt(KnowledgeTestRecordVO::getTotalCount).sum());
        summary.put("totalCorrect", history.stream().mapToInt(KnowledgeTestRecordVO::getCorrectCount).sum());
        return summary;
    }

    @Override
    public List<KnowledgeTestRecordVO> getHistory(Integer accountId) {
        return query().eq("account_id", accountId)
                .orderByDesc("created_at")
                .list()
                .stream()
                .map(this::toVO)
                .toList();
    }

    private String toDetailJson(List<KnowledgeTestAnswer> detail) {
        List<KnowledgeTestAnswer> answers = normalizeDetail(detail);
        return answers.isEmpty() ? null : JSON.toJSONString(answers);
    }

    /**
     * 清理答题明细：丢弃缺少题目 id 或题型非法的项，截断过长答案，防止脏数据撑爆 JSON 列。
     */
    private List<KnowledgeTestAnswer> normalizeDetail(List<KnowledgeTestAnswer> detail) {
        List<KnowledgeTestAnswer> answers = new ArrayList<>();
        if (detail == null) {
            return answers;
        }
        for (KnowledgeTestAnswer item : detail) {
            if (item == null || item.getQuestionId() == null) {
                continue;
            }
            String kind = item.getKind() == null ? null : TestQuestionKind.fromCode(item.getKind().trim()).getCode();
            List<String> userAnswer = new ArrayList<>();
            if (item.getUserAnswer() != null) {
                for (String answer : item.getUserAnswer()) {
                    if (answer == null) {
                        continue;
                    }
                    String trimmed = answer.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    userAnswer.add(trimmed.substring(0, Math.min(trimmed.length(), MAX_ANSWER_LENGTH)));
                    if (userAnswer.size() == MAX_ANSWER_ITEMS) {
                        break;
                    }
                }
            }
            answers.add(new KnowledgeTestAnswer(item.getQuestionId(), kind, userAnswer, item.isCorrect()));
        }
        return answers;
    }

    private KnowledgeTestRecordVO toVO(KnowledgeTestRecord record) {
        List<KnowledgeTestAnswer> detail = new ArrayList<>();
        if (record.getDetail() != null && !record.getDetail().isBlank()) {
            List<KnowledgeTestAnswer> parsed = JSON.parseArray(record.getDetail(), KnowledgeTestAnswer.class);
            if (parsed != null) {
                detail = parsed;
            }
        }
        return new KnowledgeTestRecordVO(
                record.getId(),
                record.getTypeId(),
                record.getTypeName(),
                record.getTotalCount(),
                record.getCorrectCount(),
                record.getWrongCount(),
                record.getScore(),
                record.getDurationSeconds(),
                detail,
                record.getCreatedAt());
    }
}
