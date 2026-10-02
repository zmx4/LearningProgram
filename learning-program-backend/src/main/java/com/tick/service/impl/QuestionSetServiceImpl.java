package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.QuestionSet;
import com.tick.entity.dto.QuestionSetItem;
import com.tick.entity.vo.request.QuestionSetSaveVO;
import com.tick.entity.vo.response.QuestionSetDetailVO;
import com.tick.entity.vo.response.QuestionSetVO;
import com.tick.mapper.QuestionSetItemMapper;
import com.tick.mapper.QuestionSetMapper;
import com.tick.service.QuestionSetService;
import com.tick.service.TestQuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class QuestionSetServiceImpl extends ServiceImpl<QuestionSetMapper, QuestionSet>
        implements QuestionSetService {
    private final QuestionSetItemMapper itemMapper;
    private final TestQuestionService testQuestionService;

    public QuestionSetServiceImpl(QuestionSetItemMapper itemMapper, TestQuestionService testQuestionService) {
        this.itemMapper = itemMapper;
        this.testQuestionService = testQuestionService;
    }

    @Override
    public List<QuestionSetVO> listSets() {
        return query().orderByAsc("id").list().stream().map(this::toVO).toList();
    }

    @Override
    public QuestionSetDetailVO getSetDetail(Integer id) {
        QuestionSet set = id == null ? null : getById(id);
        if (set == null) {
            return null;
        }
        List<Integer> questionIds = itemMapper.selectList(
                        new LambdaQueryWrapper<QuestionSetItem>()
                                .eq(QuestionSetItem::getSetId, id)
                                .orderByAsc(QuestionSetItem::getId))
                .stream().map(QuestionSetItem::getQuestionId).toList();
        return new QuestionSetDetailVO(set.getId(), set.getTitle(), set.getDescription(),
                set.getCreatedAt(), testQuestionService.listByIds(questionIds));
    }

    @Override
    @Transactional
    public QuestionSetVO createSet(QuestionSetSaveVO vo) {
        Normalized normalized = normalize(vo);
        QuestionSet set = new QuestionSet(null, normalized.title(), normalized.description(), LocalDateTime.now());
        save(set);
        saveItems(set.getId(), normalized.questionIds());
        return toVO(set, normalized.questionIds().size());
    }

    @Override
    @Transactional
    public QuestionSetVO updateSet(Integer id, QuestionSetSaveVO vo) {
        QuestionSet set = id == null ? null : getById(id);
        if (set == null) {
            throw new IllegalArgumentException("题集不存在");
        }
        Normalized normalized = normalize(vo);
        set.setTitle(normalized.title());
        set.setDescription(normalized.description());
        updateById(set);
        itemMapper.delete(new LambdaQueryWrapper<QuestionSetItem>()
                .eq(QuestionSetItem::getSetId, id));
        saveItems(id, normalized.questionIds());
        return toVO(set, normalized.questionIds().size());
    }

    @Override
    @Transactional
    public void deleteSet(Integer id) {
        if (id == null || getById(id) == null) {
            throw new IllegalArgumentException("题集不存在");
        }
        removeById(id);
        // db_question_set_item 对 set_id 设置了 ON DELETE CASCADE，此处兜底清理
        itemMapper.delete(new LambdaQueryWrapper<QuestionSetItem>()
                .eq(QuestionSetItem::getSetId, id));
    }

    private void saveItems(Integer setId, List<Integer> questionIds) {
        for (Integer questionId : questionIds) {
            itemMapper.insert(new QuestionSetItem(null, setId, questionId));
        }
    }

    private Normalized normalize(QuestionSetSaveVO vo) {
        if (vo == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        String title = vo.getTitle() == null ? "" : vo.getTitle().trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("题集标题不能为空");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("题集标题不能超过 100 个字符");
        }

        String description = vo.getDescription() == null ? null : vo.getDescription().trim();
        if (description != null && description.isEmpty()) {
            description = null;
        }
        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException("题集描述不能超过 255 个字符");
        }

        List<Integer> questionIds = vo.getQuestionIds() == null ? List.of()
                : List.copyOf(new LinkedHashSet<>(vo.getQuestionIds()));
        if (!questionIds.isEmpty() && testQuestionService.listByIds(questionIds).size() != questionIds.size()) {
            throw new IllegalArgumentException("题集中包含不存在的题目");
        }

        return new Normalized(title, description, questionIds);
    }

    private QuestionSetVO toVO(QuestionSet set) {
        long count = itemMapper.selectCount(
                new LambdaQueryWrapper<QuestionSetItem>()
                        .eq(QuestionSetItem::getSetId, set.getId()));
        return toVO(set, (int) count);
    }

    private QuestionSetVO toVO(QuestionSet set, int questionCount) {
        return new QuestionSetVO(set.getId(), set.getTitle(), set.getDescription(),
                questionCount, set.getCreatedAt());
    }

    private record Normalized(String title, String description, List<Integer> questionIds) {
    }
}
