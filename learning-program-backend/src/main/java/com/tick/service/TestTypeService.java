package com.tick.service;

import com.tick.entity.dto.TestType;
import com.tick.entity.vo.request.TestTypeCreateVO;

import java.util.List;

/**
 * 题目类型服务：类型 CRUD，删除前校验类型下的题目与成绩引用。
 */
public interface TestTypeService {
    /**
     * 类型列表，按创建顺序。
     */
    List<TestType> listTypes();

    /**
     * 单个类型，不存在返回 null。
     */
    TestType getType(Integer id);

    /**
     * 新建类型：code 唯一且仅允许字母数字下划线连字符。
     */
    TestType createType(TestTypeCreateVO vo);

    /**
     * 更新类型名称与描述。
     */
    TestType updateType(Integer id, TestTypeCreateVO vo);

    /**
     * 删除类型；类型下仍有题目或成绩引用时抛 {@link IllegalArgumentException}。
     */
    void deleteType(Integer id);
}
