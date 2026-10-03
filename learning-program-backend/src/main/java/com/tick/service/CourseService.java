package com.tick.service;

import com.tick.entity.vo.request.CourseSaveVO;
import com.tick.entity.vo.response.AdminCourseVO;

import java.util.List;

public interface CourseService {
    List<AdminCourseVO> listCourses();

    AdminCourseVO getCourse(Integer id);

    AdminCourseVO createCourse(CourseSaveVO vo);

    AdminCourseVO updateCourse(Integer id, CourseSaveVO vo);

    void deleteCourse(Integer id);
}
