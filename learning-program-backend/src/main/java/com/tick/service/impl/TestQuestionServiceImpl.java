package com.tick.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.QuestionSetItem;
import com.tick.entity.dto.TestQuestion;
import com.tick.entity.dto.TestQuestionContent;
import com.tick.entity.dto.TestQuestionKind;
import com.tick.entity.vo.request.TestQuestionCreateVO;
import com.tick.entity.vo.response.TestQuestionVO;
import com.tick.mapper.QuestionSetItemMapper;
import com.tick.mapper.TestQuestionMapper;
import com.tick.service.TestQuestionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 题库题目服务实现。
 */
@Service
public class TestQuestionServiceImpl extends ServiceImpl<TestQuestionMapper, TestQuestion>
        implements TestQuestionService {
    private static final int MAX_COUNT = 100;

    private final QuestionSetItemMapper questionSetItemMapper;

    public TestQuestionServiceImpl(QuestionSetItemMapper questionSetItemMapper) {
        this.questionSetItemMapper = questionSetItemMapper;
    }

    @Override
    public List<TestQuestionVO> getQuestions(Integer typeId, String kind, Integer count) {
        TestQuestionKind questionKind = resolveKind(kind);
        String kindCode = questionKind == null ? null : questionKind.getCode();

        List<TestQuestion> questions;
        if (count == null) {
            questions = query().eq("type_id", typeId)
                    .eq(questionKind != null, "kind", kindCode)
                    .orderByAsc("id")
                    .list();
        } else {
            if (count < 1 || count > MAX_COUNT) {
                throw new IllegalArgumentException("题目数量必须在 1 到 " + MAX_COUNT + " 之间");
            }
            questions = baseMapper.selectRandomByType(typeId, kindCode, count);
        }

        return questions.stream().map(this::toVO).toList();
    }

    @Override
    public List<TestQuestionVO> listByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Integer, TestQuestion> byId = baseMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(TestQuestion::getId, q -> q));
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(this::toVO)
                .toList();
    }

    @Override
    public TestQuestionVO createQuestion(Integer typeId, TestQuestionCreateVO vo) {
        if (vo == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        TestQuestionKind kind = TestQuestionKind.fromCode(vo.getKind());
        TestQuestionContent content = vo.getContent() == null ? new TestQuestionContent() : vo.getContent();
        content.normalizeAndValidate(kind);

        TestQuestion question = new TestQuestion(
                null, typeId, kind.getCode(), toContentJson(content), LocalDateTime.now());
        save(question);
        return toVO(question);
    }

    @Override
    public TestQuestionVO updateQuestion(Integer id, TestQuestionCreateVO vo) {
        TestQuestion question = id == null ? null : getById(id);
        if (question == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        if (vo == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        TestQuestionKind kind = TestQuestionKind.fromCode(vo.getKind());
        TestQuestionContent content = vo.getContent() == null ? new TestQuestionContent() : vo.getContent();
        content.normalizeAndValidate(kind);

        question.setTypeId(vo.getTypeId() == null ? question.getTypeId() : vo.getTypeId());
        question.setKind(kind.getCode());
        question.setContent(toContentJson(content));
        updateById(question);
        return toVO(question);
    }

    @Override
    public void deleteQuestion(Integer id) {
        if (id == null || getById(id) == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        Long refCount = questionSetItemMapper.selectCount(
                new LambdaQueryWrapper<QuestionSetItem>().eq(QuestionSetItem::getQuestionId, id));
        if (refCount > 0) {
            throw new IllegalArgumentException("该题目已被题集引用，请先在题集中移除");
        }
        removeById(id);
    }

    private TestQuestionKind resolveKind(String kind) {
        return kind == null || kind.isBlank() ? null : TestQuestionKind.fromCode(kind);
    }

    private TestQuestionVO toVO(TestQuestion question) {
        return new TestQuestionVO(
                question.getId(),
                question.getTypeId(),
                question.getKind(),
                parseContent(question.getContent()),
                question.getCreatedAt());
    }

    private TestQuestionContent parseContent(String raw) {
        if (raw == null || raw.isBlank()) {
            return new TestQuestionContent();
        }
        TestQuestionContent content = JSON.parseObject(raw, TestQuestionContent.class);
        return content == null ? new TestQuestionContent() : content;
    }

    private String toContentJson(TestQuestionContent content) {
        JSONObject json = new JSONObject();
        json.put("stem", content.getStem());
        json.put("options", content.getOptions());
        json.put("answer", content.getAnswer());
        json.put("analysis", content.getAnalysis());
        return JSON.toJSONString(json, JSONWriter.Feature.WriteNulls);
    }
}
