package com.tick.service;

import com.tick.entity.dto.TestType;
import com.tick.entity.vo.request.TestTypeCreateVO;

import java.util.List;

public interface TestTypeService {
    List<TestType> listTypes();

    TestType getType(Integer id);

    TestType createType(TestTypeCreateVO vo);

    TestType updateType(Integer id, TestTypeCreateVO vo);

    void deleteType(Integer id);
}
