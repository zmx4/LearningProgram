package com.tick.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.TestType;
import com.tick.entity.vo.request.TestTypeCreateVO;
import com.tick.mapper.TestTypeMapper;
import com.tick.service.TestTypeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TestTypeServiceImpl extends ServiceImpl<TestTypeMapper, TestType> implements TestTypeService {
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,50}");

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

        if (query().eq("code", code).count() > 0) {
            throw new IllegalArgumentException("测试类型编码已存在：" + code);
        }

        TestType type = new TestType(null, code, name, description, LocalDateTime.now());
        save(type);
        return type;
    }
}
