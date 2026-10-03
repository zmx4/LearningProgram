package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Course;
import com.tick.entity.dto.CourseChapter;
import com.tick.entity.dto.CourseQuestionSet;
import com.tick.entity.dto.QuestionSet;
import com.tick.entity.vo.request.CourseSaveVO;
import com.tick.entity.vo.response.AdminChapterVO;
import com.tick.entity.vo.response.AdminCourseVO;
import com.tick.mapper.CourseChapterMapper;
import com.tick.mapper.CourseMapper;
import com.tick.mapper.CourseQuestionSetMapper;
import com.tick.mapper.QuestionSetMapper;
import com.tick.service.CourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course>
        implements CourseService {
    private final CourseChapterMapper chapterMapper;
    private final CourseQuestionSetMapper questionSetLinkMapper;
    private final QuestionSetMapper questionSetMapper;

    public CourseServiceImpl(CourseChapterMapper chapterMapper,
                             CourseQuestionSetMapper questionSetLinkMapper,
                             QuestionSetMapper questionSetMapper) {
        this.chapterMapper = chapterMapper;
        this.questionSetLinkMapper = questionSetLinkMapper;
        this.questionSetMapper = questionSetMapper;
    }

    @Override
    public List<AdminCourseVO> listCourses() {
        return query().orderByAsc("sort_order").orderByAsc("id").list().stream()
                .map(course -> toVO(course, false)).toList();
    }

    @Override
    public AdminCourseVO getCourse(Integer id) {
        Course course = id == null ? null : getById(id);
        return course == null ? null : toVO(course, true);
    }

    @Override
    @Transactional
    public AdminCourseVO createCourse(CourseSaveVO vo) {
        Normalized normalized = normalize(vo);
        int sortOrder = normalized.sortOrder() != null
                ? normalized.sortOrder()
                : (int) count() + 1;
        Course course = new Course(null, normalized.title(), normalized.description(),
                normalized.icon(), sortOrder, LocalDateTime.now());
        save(course);
        saveChapters(course.getId(), normalized.chapters(), Map.of());
        saveSetLinks(course.getId(), normalized.questionSetIds());
        return toVO(course, true);
    }

    @Override
    @Transactional
    public AdminCourseVO updateCourse(Integer id, CourseSaveVO vo) {
        Course course = id == null ? null : getById(id);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        Normalized normalized = normalize(vo);
        course.setTitle(normalized.title());
        course.setDescription(normalized.description());
        course.setIcon(normalized.icon());
        if (normalized.sortOrder() != null) {
            course.setSortOrder(normalized.sortOrder());
        }
        updateById(course);

        // 带已有章节 id 的项原位更新以保留学员进度，其余插入；不在提交列表中的旧章节删除
        Map<Integer, CourseChapter> existing = chapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourseId, id))
                .stream().collect(Collectors.toMap(CourseChapter::getId, Function.identity()));
        saveChapters(id, normalized.chapters(), existing);
        Set<Integer> keptIds = normalized.chapters().stream()
                .map(CourseSaveVO.ChapterItem::getId)
                .filter(existing::containsKey)
                .collect(Collectors.toSet());
        for (Integer staleId : existing.keySet()) {
            if (!keptIds.contains(staleId)) {
                chapterMapper.deleteById(staleId);
            }
        }

        questionSetLinkMapper.delete(new LambdaQueryWrapper<CourseQuestionSet>()
                .eq(CourseQuestionSet::getCourseId, id));
        saveSetLinks(id, normalized.questionSetIds());
        return toVO(course, true);
    }

    @Override
    @Transactional
    public void deleteCourse(Integer id) {
        if (id == null || getById(id) == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        removeById(id);
        // 章节、题集关联由外键级联删除，学员进度再随章节级联删除，此处兜底清理
        chapterMapper.delete(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, id));
        questionSetLinkMapper.delete(new LambdaQueryWrapper<CourseQuestionSet>()
                .eq(CourseQuestionSet::getCourseId, id));
    }

    private void saveChapters(Integer courseId, List<CourseSaveVO.ChapterItem> chapters,
                              Map<Integer, CourseChapter> existing) {
        for (int i = 0; i < chapters.size(); i++) {
            CourseSaveVO.ChapterItem item = chapters.get(i);
            CourseChapter kept = item.getId() == null ? null : existing.get(item.getId());
            if (kept == null) {
                chapterMapper.insert(new CourseChapter(null, courseId, item.getTitle(),
                        item.getContent(), i, LocalDateTime.now()));
            } else {
                kept.setTitle(item.getTitle());
                kept.setContent(item.getContent());
                kept.setSortOrder(i);
                chapterMapper.updateById(kept);
            }
        }
    }

    private void saveSetLinks(Integer courseId, List<Integer> setIds) {
        for (int i = 0; i < setIds.size(); i++) {
            questionSetLinkMapper.insert(new CourseQuestionSet(null, courseId, setIds.get(i), i, LocalDateTime.now()));
        }
    }

    private Normalized normalize(CourseSaveVO vo) {
        if (vo == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }

        String title = vo.getTitle() == null ? "" : vo.getTitle().trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("课程标题不能为空");
        }
        if (title.length() > 100) {
            throw new IllegalArgumentException("课程标题不能超过 100 个字符");
        }

        String description = vo.getDescription() == null ? null : vo.getDescription().trim();
        if (description != null && description.isEmpty()) {
            description = null;
        }
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("课程描述不能超过 500 个字符");
        }

        String icon = vo.getIcon() == null ? null : vo.getIcon().trim();
        if (icon != null && icon.isEmpty()) {
            icon = null;
        }
        if (icon != null && icon.length() > 16) {
            throw new IllegalArgumentException("课程图标不能超过 16 个字符");
        }

        List<CourseSaveVO.ChapterItem> chapters = new ArrayList<>();
        if (vo.getChapters() != null) {
            for (CourseSaveVO.ChapterItem item : vo.getChapters()) {
                if (item == null) {
                    continue;
                }
                String chapterTitle = item.getTitle() == null ? "" : item.getTitle().trim();
                if (chapterTitle.isEmpty()) {
                    throw new IllegalArgumentException("章节标题不能为空");
                }
                if (chapterTitle.length() > 150) {
                    throw new IllegalArgumentException("章节标题不能超过 150 个字符");
                }
                String content = item.getContent() == null ? null : item.getContent().trim();
                if (content != null && content.isEmpty()) {
                    content = null;
                }
                CourseSaveVO.ChapterItem normalizedItem = new CourseSaveVO.ChapterItem();
                normalizedItem.setId(item.getId());
                normalizedItem.setTitle(chapterTitle);
                normalizedItem.setContent(content);
                chapters.add(normalizedItem);
            }
        }

        List<Integer> setIds = vo.getQuestionSetIds() == null ? List.of()
                : List.copyOf(new LinkedHashSet<>(vo.getQuestionSetIds()));
        if (!setIds.isEmpty() && questionSetMapper.selectCount(new LambdaQueryWrapper<QuestionSet>()
                .in(QuestionSet::getId, setIds)) != setIds.size()) {
            throw new IllegalArgumentException("课程中包含不存在的题集");
        }

        return new Normalized(title, description, icon, vo.getSortOrder(), chapters, setIds);
    }

    private AdminCourseVO toVO(Course course, boolean withChapters) {
        long chapterCount = chapterMapper.selectCount(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, course.getId()));
        List<Integer> setIds = questionSetLinkMapper.selectList(new LambdaQueryWrapper<CourseQuestionSet>()
                        .eq(CourseQuestionSet::getCourseId, course.getId())
                        .orderByAsc(CourseQuestionSet::getSortOrder).orderByAsc(CourseQuestionSet::getId))
                .stream().map(CourseQuestionSet::getSetId).toList();
        List<AdminChapterVO> chapters = withChapters
                ? chapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourseId, course.getId())
                        .orderByAsc(CourseChapter::getSortOrder).orderByAsc(CourseChapter::getId))
                .stream().map(chapter -> new AdminChapterVO(chapter.getId(), chapter.getTitle(),
                        chapter.getContent(), chapter.getSortOrder()))
                .toList()
                : null;
        return new AdminCourseVO(course.getId(), course.getTitle(), course.getDescription(),
                course.getIcon(), course.getSortOrder(), course.getCreatedAt(),
                (int) chapterCount, setIds, chapters);
    }

    private record Normalized(String title, String description, String icon, Integer sortOrder,
                              List<CourseSaveVO.ChapterItem> chapters, List<Integer> questionSetIds) {
    }
}
