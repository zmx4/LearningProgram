package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 题集详情，questions 按题目加入题集的顺序排列。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSetDetailVO {
    private Integer id;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private List<TestQuestionVO> questions;
}
