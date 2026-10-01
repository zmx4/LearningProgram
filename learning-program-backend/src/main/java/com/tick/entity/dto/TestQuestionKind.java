package com.tick.entity.dto;

import lombok.Getter;

/**
 * 题型。code 为对外（接口与数据库）使用的取值。
 */
@Getter
public enum TestQuestionKind {
    SINGLE("single", "单选"),
    MULTIPLE("multiple", "多选"),
    BLANK("blank", "填空");

    private final String code;
    private final String label;

    TestQuestionKind(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static TestQuestionKind fromCode(String code) {
        if (code != null) {
            String normalized = code.trim();
            for (TestQuestionKind kind : values()) {
                if (kind.code.equalsIgnoreCase(normalized)) {
                    return kind;
                }
            }
        }
        throw new IllegalArgumentException("题型只能是 single（单选）、multiple（多选）或 blank（填空）");
    }
}
