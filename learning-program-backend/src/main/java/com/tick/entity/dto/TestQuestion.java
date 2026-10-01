package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试题目。题目内容以 JSON 存放在 content 列，结构与 {@link TestQuestionContent} 对应。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_test_question")
public class TestQuestion {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("type_id")
    private Integer typeId;
    private String kind;
    private String content;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
