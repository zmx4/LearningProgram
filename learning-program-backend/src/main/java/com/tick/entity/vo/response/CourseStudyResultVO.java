package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单次学习上报后的最新进度，用于前端更新当前课程的进度展示。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseStudyResultVO {
    private Integer studiedCount;
    private Integer totalCount;
    private String status;
}
