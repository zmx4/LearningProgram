package com.tick.service;

import com.tick.entity.vo.request.CourseSaveVO;
import com.tick.entity.vo.response.AdminCourseVO;

import java.util.List;

/**
 * 课程管理服务（管理端 CRUD）：课程与章节、题集关联的全量覆盖保存。
 */
public interface CourseService {
    /**
     * 课程列表（不含章节详情），按 sort_order 升序。
     */
    List<AdminCourseVO> listCourses();

    /**
     * 课程详情（含完整章节与关联题集 id），课程不存在返回 null。
     */
    AdminCourseVO getCourse(Integer id);

    /**
     * 创建课程并保存章节与题集关联；sortOrder 缺省排到末尾。
     */
    AdminCourseVO createCourse(CourseSaveVO vo);

    /**
     * 更新课程并全量覆盖章节与题集关联：带已有章节 id 的项原位更新以保留学员进度，
     * 不在提交列表中的旧章节删除。
     */
    AdminCourseVO updateCourse(Integer id, CourseSaveVO vo);

    /**
     * 删除课程，章节、题集关联与学员进度随之级联清理。
     */
    void deleteCourse(Integer id);
}
