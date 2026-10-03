package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 我的课程列表项：一门课程的学习进度。status 取值为
 * not_started（未开始）、in_progress（进行中）、completed（已完成）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressVO {
    private Integer courseId;
    private String title;
    private String description;
    private String icon;
    private Integer chapterCount;
    private Integer studiedCount;
    private String status;
    private Integer totalSeconds;
    private LocalDateTime lastStudiedAt;
    private LocalDateTime completedAt;
}
