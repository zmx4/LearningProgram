package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.request.CourseStudyRecordVO;
import com.tick.entity.vo.response.CourseDetailVO;
import com.tick.entity.vo.response.CourseProgressOverviewVO;
import com.tick.entity.vo.response.CourseStudyProgressVO;
import com.tick.entity.vo.response.CourseStudyResultVO;
import com.tick.service.CourseProgressService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程学习进度接口。登录用户可查看课程与自己的进度，并上报章节学习记录。
 */
@RestController
@RequestMapping("/api/courses")
public class CourseProgressController {
    private final CourseProgressService courseProgressService;

    public CourseProgressController(CourseProgressService courseProgressService) {
        this.courseProgressService = courseProgressService;
    }

    @GetMapping("/progress")
    public RestBean<CourseProgressOverviewVO> overview(HttpServletRequest request) {
        Integer accountId = (Integer) request.getAttribute("id");
        if (accountId == null) return RestBean.unauthorized("登录状态无效");
        return RestBean.success(courseProgressService.getOverview(accountId));
    }

    @GetMapping("/{courseId}")
    public RestBean<CourseDetailVO> detail(@PathVariable Integer courseId) {
        CourseDetailVO detail = courseProgressService.getCourseDetail(courseId);
        if (detail == null) {
            return RestBean.failure(404, "课程不存在");
        }
        return RestBean.success(detail);
    }

    @GetMapping("/{courseId}/progress")
    public RestBean<CourseStudyProgressVO> courseProgress(@PathVariable Integer courseId, HttpServletRequest request) {
        Integer accountId = (Integer) request.getAttribute("id");
        if (accountId == null) return RestBean.unauthorized("登录状态无效");
        CourseStudyProgressVO progress = courseProgressService.getCourseProgress(accountId, courseId);
        if (progress == null) {
            return RestBean.failure(404, "课程不存在");
        }
        return RestBean.success(progress);
    }

    @PostMapping("/{courseId}/progress")
    public RestBean<CourseStudyResultVO> record(@PathVariable Integer courseId,
                                                @RequestBody CourseStudyRecordVO vo,
                                                HttpServletRequest request) {
        Integer accountId = (Integer) request.getAttribute("id");
        if (accountId == null) return RestBean.unauthorized("登录状态无效");
        try {
            return RestBean.success(courseProgressService.recordStudy(accountId, courseId, vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }
}
