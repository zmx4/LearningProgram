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
 * 课程，章节内容存放在 {@link CourseChapter}，练习题集通过 db_course_question_set 关联。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private String description;
    private String icon;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
