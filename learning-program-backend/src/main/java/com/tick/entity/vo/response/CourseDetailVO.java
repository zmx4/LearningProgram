package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 课程详情：章节按 sort_order 排列，questionSets 为挂载的练习题集（按课程内顺序）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailVO {
    private Integer id;
    private String title;
    private String description;
    private String icon;
    private List<CourseQuestionSetVO> questionSets;
    private List<CourseChapterVO> chapters;
}
