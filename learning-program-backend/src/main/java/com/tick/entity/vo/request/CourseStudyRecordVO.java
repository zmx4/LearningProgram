package com.tick.entity.vo.request;

import lombok.Data;

/**
 * 前端上报的单章学习记录。
 */
@Data
public class CourseStudyRecordVO {
    private Integer chapterId;
    private Integer durationSeconds;
}
