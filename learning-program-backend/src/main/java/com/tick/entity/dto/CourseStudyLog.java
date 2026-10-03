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
 * 单次课程学习的流水记录，每次上报学习时长追加一行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_course_study_log")
public class CourseStudyLog {
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
    @TableField("created_at")
    private LocalDateTime createdAt;
}
