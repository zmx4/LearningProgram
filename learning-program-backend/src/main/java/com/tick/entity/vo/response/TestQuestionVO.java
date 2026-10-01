package com.tick.entity.vo.response;

import com.tick.entity.dto.TestQuestionContent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 返回给前端的题目，content 由数据库中的 JSON 解析而来。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestQuestionVO {
    private Integer id;
    private Integer typeId;
    private String kind;
    private TestQuestionContent content;
    private LocalDateTime createdAt;
}
