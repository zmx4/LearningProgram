package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单门课程的学习进度，studiedChapterIds 供学习页标记已学章节。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseStudyProgressVO {
    private Integer courseId;
    private Integer chapterCount;
    private Integer studiedCount;
    private String status;
    private java.util.List<Integer> studiedChapterIds;
}
