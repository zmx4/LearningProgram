package com.tick.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 选择题的一个选项。填空题没有选项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOption {
    private String key;
    private String text;
}
