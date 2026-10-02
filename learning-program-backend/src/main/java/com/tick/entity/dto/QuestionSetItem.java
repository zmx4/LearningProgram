package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 题集与题目的关联记录，id 顺序即题目在题集中的顺序。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_question_set_item")
public class QuestionSetItem {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("set_id")
    private Integer setId;
    @TableField("question_id")
    private Integer questionId;
}
