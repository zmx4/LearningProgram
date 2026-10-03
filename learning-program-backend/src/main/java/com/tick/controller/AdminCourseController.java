package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.request.CourseSaveVO;
import com.tick.entity.vo.response.AdminCourseVO;
import com.tick.service.CourseService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课程的读写接口。挂在 /api/admin 下，由 SecurityConfiguration 限定为 admin 角色。
 */
@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {
    private final CourseService courseService;

    public AdminCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public RestBean<List<AdminCourseVO>> listCourses() {
        return RestBean.success(courseService.listCourses());
    }

    @GetMapping("/{id}")
    public RestBean<AdminCourseVO> getCourse(@PathVariable Integer id) {
        AdminCourseVO course = courseService.getCourse(id);
        if (course == null) {
            return RestBean.failure(404, "课程不存在");
        }
        return RestBean.success(course);
    }

    @PostMapping
    public RestBean<AdminCourseVO> createCourse(@RequestBody CourseSaveVO vo) {
        try {
            return RestBean.success(courseService.createCourse(vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @PutMapping("/{id}")
    public RestBean<AdminCourseVO> updateCourse(@PathVariable Integer id, @RequestBody CourseSaveVO vo) {
        try {
            return RestBean.success(courseService.updateCourse(id, vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public RestBean<Void> deleteCourse(@PathVariable Integer id) {
        try {
            courseService.deleteCourse(id);
            return RestBean.success();
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }
}
