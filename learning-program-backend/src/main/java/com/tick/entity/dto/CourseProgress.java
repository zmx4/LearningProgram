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
 * 课程学习进度，一个用户对某课程一个章节的记录：学过几次、累计学了多久。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_course_progress")
public class CourseProgress {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("account_id")
    private Integer accountId;
    @TableField("course_id")
    private Integer courseId;
    @TableField("chapter_id")
    private Integer chapterId;
    @TableField("duration_seconds")
    private Integer durationSeconds;
    @TableField("studied_count")
    private Integer studiedCount;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
