package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课程章节，返回给学习页阅读。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseChapterVO {
    private Integer id;
    private String title;
    private String content;
}
