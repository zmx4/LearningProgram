package com.tick.entity.dto;

import lombok.Getter;

/**
 * 测试大类。code 为数据库 db_test_record.category 使用的取值。
 */
@Getter
public enum TestCategory {
    WORD("word", "单词测试"),
    KNOWLEDGE("knowledge", "知识测试");

    private final String code;
    private final String label;

    TestCategory(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static TestCategory fromCode(String code) {
        if (code != null) {
            String normalized = code.trim();
            for (TestCategory category : values()) {
                if (category.code.equalsIgnoreCase(normalized)) {
                    return category;
                }
            }
        }
        throw new IllegalArgumentException("测试大类只能是 word（单词测试）或 knowledge（知识测试）");
    }
}
