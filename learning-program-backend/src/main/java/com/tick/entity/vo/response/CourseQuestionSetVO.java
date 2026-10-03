package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课程挂载的练习题集，返回给学习页作为练习入口，questionCount 为题集中的题目数量。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseQuestionSetVO {
    private Integer id;
    private String title;
    private String description;
    private Integer questionCount;
}
