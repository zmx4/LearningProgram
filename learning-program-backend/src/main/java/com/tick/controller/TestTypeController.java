package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.dto.TestType;
import com.tick.service.TestTypeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 测试类型的读取接口，登录后即可访问。
 */
@RestController
@RequestMapping("/api/test-types")
public class TestTypeController {
    private final TestTypeService testTypeService;

    public TestTypeController(TestTypeService testTypeService) {
        this.testTypeService = testTypeService;
    }

    @GetMapping
    public RestBean<List<TestType>> types() {
        return RestBean.success(testTypeService.listTypes());
    }

    @GetMapping("/{id}")
    public RestBean<TestType> type(@PathVariable Integer id) {
        TestType type = testTypeService.getType(id);
        return type == null ? RestBean.failure(404, "测试类型不存在") : RestBean.success(type);
    }
}
