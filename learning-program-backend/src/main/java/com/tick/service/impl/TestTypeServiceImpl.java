package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.KnowledgeTestRecord;
import com.tick.entity.dto.TestQuestion;
import com.tick.entity.dto.TestType;
import com.tick.entity.vo.request.TestTypeCreateVO;
import com.tick.mapper.KnowledgeTestRecordMapper;
import com.tick.mapper.TestQuestionMapper;
import com.tick.mapper.TestTypeMapper;
import com.tick.service.TestTypeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 题目类型服务实现。
 */
@Service
public class TestTypeServiceImpl extends ServiceImpl<TestTypeMapper, TestType> implements TestTypeService {
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,50}");

    private final TestQuestionMapper testQuestionMapper;
    private final KnowledgeTestRecordMapper knowledgeTestRecordMapper;

    public TestTypeServiceImpl(TestQuestionMapper testQuestionMapper,
                               KnowledgeTestRecordMapper knowledgeTestRecordMapper) {
        this.testQuestionMapper = testQuestionMapper;
        this.knowledgeTestRecordMapper = knowledgeTestRecordMapper;
    }

    @Override
    public List<TestType> listTypes() {
        return query().orderByAsc("id").list();
    }

    @Override
    public TestType getType(Integer id) {
        return id == null ? null : getById(id);
    }

    @Override
    public TestType createType(TestTypeCreateVO vo) {
        Normalized normalized = normalize(vo);
        if (query().eq("code", normalized.code()).count() > 0) {
            throw new IllegalArgumentException("测试类型编码已存在：" + normalized.code());
        }

        TestType type = new TestType(null, normalized.code(), normalized.name(), normalized.description(), LocalDateTime.now());
        save(type);
        return type;
    }

    @Override
    public TestType updateType(Integer id, TestTypeCreateVO vo) {
        TestType type = id == null ? null : getById(id);
        if (type == null) {
            throw new IllegalArgumentException("测试类型不存在");
        }

        Normalized normalized = normalize(vo);
        TestType existing = query().eq("code", normalized.code()).one();
        if (existing != null && !existing.getId().equals(id)) {
            throw new IllegalArgumentException("测试类型编码已存在：" + normalized.code());
        }

        type.setCode(normalized.code());
        type.setName(normalized.name());
        type.setDescription(normalized.description());
        updateById(type);
        return type;
    }

    @Override
    public void deleteType(Integer id) {
        if (id == null || getById(id) == null) {
            throw new IllegalArgumentException("测试类型不存在");
        }
        Long questionCount = testQuestionMapper.selectCount(
                new LambdaQueryWrapper<TestQuestion>()
                        .eq(TestQuestion::getTypeId, id));
        if (questionCount > 0) {
            throw new IllegalArgumentException("该类型下仍有题目，请先删除或转移题目");
        }
        Long recordCount = knowledgeTestRecordMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeTestRecord>()
                        .eq(KnowledgeTestRecord::getTypeId, id));
        if (recordCount > 0) {
            throw new IllegalArgumentException("该类型已有测试记录，无法删除");
        }
        removeById(id);
    }

    private Normalized normalize(TestTypeCreateVO vo) {
        if (vo == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        String code = vo.getCode() == null ? "" : vo.getCode().trim();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("测试类型编码不能为空");
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("测试类型编码只能包含字母、数字、下划线和短横线，且长度为 1 到 50 个字符");
        }

        String name = vo.getName() == null ? "" : vo.getName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("测试类型名称不能为空");
        }
        if (name.length() > 100) {
            throw new IllegalArgumentException("测试类型名称不能超过 100 个字符");
        }

        String description = vo.getDescription() == null ? null : vo.getDescription().trim();
        if (description != null && description.isEmpty()) {
            description = null;
        }
        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException("测试类型描述不能超过 255 个字符");
        }

        return new Normalized(code, name, description);
    }

    private record Normalized(String code, String name, String description) {
    }
}
