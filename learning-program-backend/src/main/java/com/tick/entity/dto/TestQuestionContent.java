package com.tick.entity.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 题目内容，序列化后存放在 db_test_question.content 这个 JSON 列中。
 * <p>
 * 三种题型共用同一个结构，靠 {@link TestQuestionKind} 区分语义：
 * <ul>
 *     <li>single / multiple：{@code options} 为选项，{@code answer} 为正确选项的 key</li>
 *     <li>blank：{@code options} 必须为空，{@code answer} 按空格顺序每空一项</li>
 * </ul>
 */
@Data
public class TestQuestionContent {
    private static final int MAX_STEM_LENGTH = 1000;
    private static final int MAX_ANALYSIS_LENGTH = 1000;
    private static final int MAX_OPTION_COUNT = 10;
    private static final int MAX_BLANK_COUNT = 10;

    private String stem;
    private List<QuestionOption> options;
    private List<String> answer;
    private String analysis;

    /**
     * 校验内容与题型是否匹配，并就地清理首尾空白、补齐 null 集合。
     *
     * @throws IllegalArgumentException 内容不合法时抛出，message 可直接返回给前端
     */
    public void normalizeAndValidate(TestQuestionKind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("题型无效");
        }

        stem = stem == null ? "" : stem.trim();
        if (stem.isEmpty()) {
            throw new IllegalArgumentException("题干不能为空");
        }
        if (stem.length() > MAX_STEM_LENGTH) {
            throw new IllegalArgumentException("题干不能超过 " + MAX_STEM_LENGTH + " 个字符");
        }

        if (analysis != null) {
            analysis = analysis.trim();
            if (analysis.isEmpty()) {
                analysis = null;
            } else if (analysis.length() > MAX_ANALYSIS_LENGTH) {
                throw new IllegalArgumentException("解析不能超过 " + MAX_ANALYSIS_LENGTH + " 个字符");
            }
        }

        List<String> answers = new ArrayList<>();
        if (answer != null) {
            for (String item : answer) {
                answers.add(item == null ? "" : item.trim());
            }
        }
        answer = answers;

        if (kind == TestQuestionKind.BLANK) {
            normalizeBlank();
        } else {
            normalizeChoice(kind);
        }
    }

    private void normalizeBlank() {
        if (options != null && !options.isEmpty()) {
            throw new IllegalArgumentException("填空题不能包含选项");
        }
        options = List.of();

        if (answer.isEmpty()) {
            throw new IllegalArgumentException("填空题答案不能为空");
        }
        if (answer.size() > MAX_BLANK_COUNT) {
            throw new IllegalArgumentException("填空题的空数不能超过 " + MAX_BLANK_COUNT + " 个");
        }
        for (String item : answer) {
            if (item.isEmpty()) {
                throw new IllegalArgumentException("填空题答案不能为空");
            }
        }
    }

    private void normalizeChoice(TestQuestionKind kind) {
        if (options == null) {
            options = new ArrayList<>();
        }
        if (options.size() < 2) {
            throw new IllegalArgumentException("选择题至少需要 2 个选项");
        }
        if (options.size() > MAX_OPTION_COUNT) {
            throw new IllegalArgumentException("选择题的选项不能超过 " + MAX_OPTION_COUNT + " 个");
        }

        Set<String> keys = new LinkedHashSet<>();
        for (QuestionOption option : options) {
            if (option == null) {
                throw new IllegalArgumentException("选项不能为空");
            }
            option.setKey(option.getKey() == null ? "" : option.getKey().trim());
            option.setText(option.getText() == null ? "" : option.getText().trim());
            if (option.getKey().isEmpty()) {
                throw new IllegalArgumentException("选项标识不能为空");
            }
            if (option.getText().isEmpty()) {
                throw new IllegalArgumentException("选项内容不能为空");
            }
            if (!keys.add(option.getKey())) {
                throw new IllegalArgumentException("选项标识不能重复：" + option.getKey());
            }
        }

        if (answer.isEmpty()) {
            throw new IllegalArgumentException("选择题答案不能为空");
        }
        for (String item : answer) {
            if (!keys.contains(item)) {
                throw new IllegalArgumentException("答案必须是选项标识之一：" + item);
            }
        }
        if (new LinkedHashSet<>(answer).size() != answer.size()) {
            throw new IllegalArgumentException("答案不能重复");
        }
        if (kind == TestQuestionKind.SINGLE && answer.size() != 1) {
            throw new IllegalArgumentException("单选题只能有一个答案");
        }
        if (kind == TestQuestionKind.MULTIPLE && answer.size() < 2) {
            throw new IllegalArgumentException("多选题至少需要两个答案");
        }
    }
}
