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
 * 课程与练习题集的关联记录。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_course_question_set")
public class CourseQuestionSet {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("course_id")
    private Integer courseId;
    @TableField("set_id")
    private Integer setId;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
