package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 题集列表项，附带题目数量。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSetVO {
    private Integer id;
    private String title;
    private String description;
    private Integer questionCount;
    private LocalDateTime createdAt;
}
