package com.tick.service.impl;

import com.alibaba.fastjson2.JSON;
import com.tick.entity.dto.KnowledgeTestAnswer;
import com.tick.entity.dto.QuestionSet;
import com.tick.entity.dto.TestCategory;
import com.tick.entity.dto.TestQuestionKind;
import com.tick.entity.dto.TestRecord;
import com.tick.entity.dto.TestType;
import com.tick.entity.vo.request.KnowledgeTestResultVO;
import com.tick.entity.vo.response.KnowledgeTestRecordVO;
import com.tick.mapper.QuestionSetMapper;
import com.tick.service.KnowledgeTestService;
import com.tick.service.TestTypeService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识测试服务实现。公共的成绩校验、计分、历史与汇总逻辑见父类 {@link BaseTestService}。
 */
@Service
public class KnowledgeTestServiceImpl extends BaseTestService implements KnowledgeTestService {
    private static final int MAX_TOTAL_COUNT = 100;
    private static final int MAX_ANSWER_LENGTH = 200;
    private static final int MAX_ANSWER_ITEMS = 10;

    private final TestTypeService testTypeService;
    private final QuestionSetMapper questionSetMapper;

    public KnowledgeTestServiceImpl(TestTypeService testTypeService, QuestionSetMapper questionSetMapper) {
        this.testTypeService = testTypeService;
        this.questionSetMapper = questionSetMapper;
    }

    @Override
    protected TestCategory category() {
        return TestCategory.KNOWLEDGE;
    }

    @Override
    public KnowledgeTestRecordVO saveResult(Integer accountId, KnowledgeTestResultVO vo) {
        if (vo == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (vo.getSetId() == null && vo.getTypeId() == null) {
            throw new IllegalArgumentException("typeId 与 setId 必须传其一");
        }
        if (vo.getSetId() != null && vo.getTypeId() != null) {
            throw new IllegalArgumentException("typeId 与 setId 只能传其一");
        }
        // typeId 普通模式与 setId 课程题集模式二选一，typeName 作为快照落库供历史展示
        TestType type = null;
        QuestionSet set = null;
        if (vo.getSetId() != null) {
            set = questionSetMapper.selectById(vo.getSetId());
            if (set == null) {
                throw new IllegalArgumentException("题集不存在");
            }
        } else {
            type = testTypeService.getType(vo.getTypeId());
            if (type == null) {
                throw new IllegalArgumentException("测试类型不存在");
            }
        }
        validateCounts(vo.getTotalCount(), vo.getCorrectCount(), MAX_TOTAL_COUNT);

        TestRecord record = buildRecord(accountId, vo.getTotalCount(), vo.getCorrectCount(),
                vo.getDurationSeconds());
        record.setTypeId(type == null ? null : type.getId());
        record.setSetId(set == null ? null : set.getId());
        record.setTypeName(type == null ? "题集：" + set.getTitle() : type.getName());
        record.setDetail(toDetailJson(vo.getDetail()));
        save(record);
        return toVO(record);
    }

    @Override
    public Map<String, Object> getSummary(Integer accountId) {
        return summarize(history(accountId));
    }

    @Override
    public List<KnowledgeTestRecordVO> getHistory(Integer accountId) {
        return history(accountId).stream().map(this::toVO).toList();
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

    private KnowledgeTestRecordVO toVO(TestRecord record) {
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
                record.getSetId(),
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
