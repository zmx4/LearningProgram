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
 * 题集，将若干测试题目组织为一个集合，例如「第一章练习」。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_question_set")
public class QuestionSet {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String description;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
