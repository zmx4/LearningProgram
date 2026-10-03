package com.tick.service;

import com.tick.entity.vo.request.CourseStudyRecordVO;
import com.tick.entity.vo.response.CourseDetailVO;
import com.tick.entity.vo.response.CourseProgressOverviewVO;
import com.tick.entity.vo.response.CourseStudyProgressVO;
import com.tick.entity.vo.response.CourseStudyResultVO;

public interface CourseProgressService {
    /**
     * 查询学习进度总览：主页统计与我的课程列表。
     */
    CourseProgressOverviewVO getOverview(Integer accountId);

    /**
     * 查询课程详情（含章节正文），供学习页使用。
     */
    CourseDetailVO getCourseDetail(Integer courseId);

    /**
     * 查询单门课程的学习进度，含已学章节 id 列表。
     */
    CourseStudyProgressVO getCourseProgress(Integer accountId, Integer courseId);

    /**
     * 上报一次单章学习记录，返回该课程的最新进度。
     */
    CourseStudyResultVO recordStudy(Integer accountId, Integer courseId, CourseStudyRecordVO vo);
}
