package com.tick.entity.vo.request;

import lombok.Data;

/**
 * 新增测试类型的请求体。
 */
@Data
public class TestTypeCreateVO {
    private String code;
    private String name;
    private String description;
}
