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
 * 课程章节，content 为章节正文，(course_id, sort_order) 决定章节顺序。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_course_chapter")
public class CourseChapter {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("course_id")
    private Integer courseId;
    private String title;
    private String content;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
