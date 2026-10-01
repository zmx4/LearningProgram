package com.tick.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识测试的答题明细项，序列化后存放在 db_knowledge_test_record.detail 这个 JSON 列中。
 * <ul>
 *     <li>single：{@code userAnswer} 恰好 1 项，为选中的选项 key</li>
 *     <li>multiple：{@code userAnswer} 为选中的全部选项 key</li>
 *     <li>blank：{@code userAnswer} 按空格顺序每空一项</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeTestAnswer {
    private Integer questionId;
    private String kind;
    private List<String> userAnswer;
    private boolean correct;
}
