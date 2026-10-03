package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学习进度总览：主页统计 + 我的课程列表，一次请求全部返回。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressOverviewVO {
    private CourseProgressSummaryVO summary;
    private List<CourseProgressVO> courses;
}
