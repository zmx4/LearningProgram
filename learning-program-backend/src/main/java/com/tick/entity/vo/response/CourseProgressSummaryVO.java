package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 主页学习概况统计：进行中/完成的课程数与本周学习时长。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressSummaryVO {
    private Integer ongoingCount;
    private Integer completedCount;
    private Integer weeklySeconds;
}
