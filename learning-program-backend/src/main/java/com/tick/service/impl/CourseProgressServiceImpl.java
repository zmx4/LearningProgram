package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Course;
import com.tick.entity.dto.CourseChapter;
import com.tick.entity.dto.CourseProgress;
import com.tick.entity.dto.CourseQuestionSet;
import com.tick.entity.dto.CourseReward;
import com.tick.entity.dto.CourseStudyLog;
import com.tick.entity.dto.QuestionSet;
import com.tick.entity.dto.QuestionSetItem;
import com.tick.entity.vo.request.CourseStudyRecordVO;
import com.tick.entity.vo.response.CourseChapterVO;
import com.tick.entity.vo.response.CourseDetailVO;
import com.tick.entity.vo.response.CourseProgressOverviewVO;
import com.tick.entity.vo.response.CourseProgressSummaryVO;
import com.tick.entity.vo.response.CourseProgressVO;
import com.tick.entity.vo.response.CourseQuestionSetVO;
import com.tick.entity.vo.response.CourseStudyProgressVO;
import com.tick.entity.vo.response.CourseStudyResultVO;
import com.tick.mapper.CourseChapterMapper;
import com.tick.mapper.CourseMapper;
import com.tick.mapper.CourseProgressMapper;
import com.tick.mapper.CourseQuestionSetMapper;
import com.tick.mapper.CourseRewardMapper;
import com.tick.mapper.CourseStudyLogMapper;
import com.tick.mapper.QuestionSetItemMapper;
import com.tick.mapper.QuestionSetMapper;
import com.tick.service.AccountPointsService;
import com.tick.service.CourseProgressService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 课程学习进度服务实现。
 */
@Service
public class CourseProgressServiceImpl extends ServiceImpl<CourseProgressMapper, CourseProgress>
        implements CourseProgressService {
    private static final String STATUS_NOT_STARTED = "not_started";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_COMPLETED = "completed";

    // 单次上报的学习时长上限（秒），防止前端异常数据污染统计
    private static final int MAX_REPORT_SECONDS = 3600;

    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseQuestionSetMapper questionSetMapper;
    private final CourseStudyLogMapper studyLogMapper;
    private final QuestionSetMapper setMapper;
    private final QuestionSetItemMapper setItemMapper;
    private final CourseRewardMapper courseRewardMapper;
    private final AccountPointsService accountPointsService;

    public CourseProgressServiceImpl(CourseMapper courseMapper,
                                     CourseChapterMapper chapterMapper,
                                     CourseQuestionSetMapper questionSetMapper,
                                     CourseStudyLogMapper studyLogMapper,
                                     QuestionSetMapper setMapper,
                                     QuestionSetItemMapper setItemMapper,
                                     CourseRewardMapper courseRewardMapper,
                                     AccountPointsService accountPointsService) {
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.questionSetMapper = questionSetMapper;
        this.studyLogMapper = studyLogMapper;
        this.setMapper = setMapper;
        this.setItemMapper = setItemMapper;
        this.courseRewardMapper = courseRewardMapper;
        this.accountPointsService = accountPointsService;
    }

    @Override
    public CourseProgressOverviewVO getOverview(Integer accountId) {
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .orderByAsc(Course::getSortOrder).orderByAsc(Course::getId));
        Map<Integer, List<CourseChapter>> chaptersByCourse = chaptersGroupedByCourse();
        List<CourseProgress> rows = accountId == null ? List.of() : lambdaQuery()
                .eq(CourseProgress::getAccountId, accountId).list();
        Map<Integer, List<CourseProgress>> rowsByCourse = rows.stream()
                .collect(Collectors.groupingBy(CourseProgress::getCourseId));

        List<CourseProgressVO> items = new ArrayList<>();
        int ongoing = 0;
        int completed = 0;
        for (Course course : courses) {
            List<CourseChapter> chapters = chaptersByCourse.getOrDefault(course.getId(), List.of());
            CourseProgressVO vo = toVO(course, chapters, rowsByCourse.getOrDefault(course.getId(), List.of()));
            if (STATUS_IN_PROGRESS.equals(vo.getStatus())) {
                ongoing += 1;
            } else if (STATUS_COMPLETED.equals(vo.getStatus())) {
                completed += 1;
            }
            items.add(vo);
        }

        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        int weeklySeconds = accountId == null ? 0 : studyLogMapper.selectList(new LambdaQueryWrapper<CourseStudyLog>()
                        .eq(CourseStudyLog::getAccountId, accountId)
                        .ge(CourseStudyLog::getCreatedAt, weekStart))
                .stream().mapToInt(CourseStudyLog::getDurationSeconds).sum();

        return new CourseProgressOverviewVO(new CourseProgressSummaryVO(ongoing, completed, weeklySeconds), items);
    }

    @Override
    public CourseDetailVO getCourseDetail(Integer courseId) {
        Course course = courseId == null ? null : courseMapper.selectById(courseId);
        if (course == null) {
            return null;
        }
        List<CourseChapterVO> chapters = chapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourseId, courseId)
                        .orderByAsc(CourseChapter::getSortOrder).orderByAsc(CourseChapter::getId))
                .stream().map(chapter -> new CourseChapterVO(chapter.getId(), chapter.getTitle(), chapter.getContent()))
                .toList();
        List<CourseQuestionSetVO> questionSets = questionSetMapper.selectList(new LambdaQueryWrapper<CourseQuestionSet>()
                        .eq(CourseQuestionSet::getCourseId, courseId)
                        .orderByAsc(CourseQuestionSet::getSortOrder).orderByAsc(CourseQuestionSet::getId))
                .stream().map(this::toQuestionSetVO).filter(Objects::nonNull).toList();
        return new CourseDetailVO(course.getId(), course.getTitle(), course.getDescription(), course.getIcon(),
                course.getRewardPoints(), questionSets, chapters);
    }

    private CourseQuestionSetVO toQuestionSetVO(CourseQuestionSet link) {
        QuestionSet set = link.getSetId() == null ? null : setMapper.selectById(link.getSetId());
        if (set == null) {
            // 关联表的 set_id 外键保证题集存在，此处兜底跳过脏数据
            return null;
        }
        long questionCount = setItemMapper.selectCount(new LambdaQueryWrapper<QuestionSetItem>()
                .eq(QuestionSetItem::getSetId, set.getId()));
        return new CourseQuestionSetVO(set.getId(), set.getTitle(), set.getDescription(), (int) questionCount);
    }

    @Override
    public CourseStudyProgressVO getCourseProgress(Integer accountId, Integer courseId) {
        Course course = courseId == null ? null : courseMapper.selectById(courseId);
        if (course == null) {
            return null;
        }
        List<Integer> chapterIds = chapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                        .eq(CourseChapter::getCourseId, courseId))
                .stream().map(CourseChapter::getId).toList();
        List<CourseProgress> rows = lambdaQuery()
                .eq(CourseProgress::getAccountId, accountId)
                .eq(CourseProgress::getCourseId, courseId).list();
        HashSet<Integer> inCourse = new HashSet<>(chapterIds);
        List<Integer> studiedIds = rows.stream().map(CourseProgress::getChapterId)
                .filter(inCourse::contains).sorted().toList();
        return new CourseStudyProgressVO(courseId, chapterIds.size(), studiedIds.size(),
                statusOf(studiedIds.size(), chapterIds.size()), studiedIds);
    }

    @Override
    @Transactional
    public CourseStudyResultVO recordStudy(Integer accountId, Integer courseId, CourseStudyRecordVO vo) {
        Course course = courseId == null ? null : courseMapper.selectById(courseId);
        if (course == null) {
            throw new IllegalArgumentException("课程不存在");
        }
        if (vo == null || vo.getChapterId() == null) {
            throw new IllegalArgumentException("章节不能为空");
        }
        CourseChapter chapter = chapterMapper.selectById(vo.getChapterId());
        if (chapter == null || !courseId.equals(chapter.getCourseId())) {
            throw new IllegalArgumentException("章节不属于该课程");
        }
        int duration = vo.getDurationSeconds() == null ? 0
                : Math.min(Math.max(vo.getDurationSeconds(), 0), MAX_REPORT_SECONDS);

        LocalDateTime now = LocalDateTime.now();
        CourseProgress progress = lambdaQuery()
                .eq(CourseProgress::getAccountId, accountId)
                .eq(CourseProgress::getChapterId, vo.getChapterId()).one();
        if (progress == null) {
            save(new CourseProgress(null, accountId, courseId, vo.getChapterId(), duration, 1, now, now));
        } else {
            progress.setDurationSeconds(progress.getDurationSeconds() + duration);
            progress.setStudiedCount(progress.getStudiedCount() + 1);
            progress.setUpdatedAt(now);
            updateById(progress);
        }
        studyLogMapper.insert(new CourseStudyLog(null, accountId, courseId, vo.getChapterId(), duration, now));

        int chapterCount = Math.toIntExact(chapterMapper.selectCount(new LambdaQueryWrapper<CourseChapter>()
                .eq(CourseChapter::getCourseId, courseId)));
        int studied = Math.toIntExact(lambdaQuery()
                .eq(CourseProgress::getAccountId, accountId)
                .eq(CourseProgress::getCourseId, courseId).count());
        String status = statusOf(studied, chapterCount);
        Integer awardedPoints = STATUS_COMPLETED.equals(status)
                ? awardCourseRewardOnce(accountId, course)
                : null;
        return new CourseStudyResultVO(studied, chapterCount, status, awardedPoints);
    }

    /**
     * 课程完成时发放奖励积分；(account_id, course_id) 唯一保证只发一次，返回本次实际发放的积分。
     */
    private Integer awardCourseRewardOnce(Integer accountId, Course course) {
        int reward = course.getRewardPoints() == null ? 0 : course.getRewardPoints();
        if (reward <= 0) {
            return null;
        }
        Long granted = courseRewardMapper.selectCount(new LambdaQueryWrapper<CourseReward>()
                .eq(CourseReward::getAccountId, accountId)
                .eq(CourseReward::getCourseId, course.getId()));
        if (granted != null && granted > 0) {
            return null;
        }
        try {
            courseRewardMapper.insert(new CourseReward(null, accountId, course.getId(), reward, LocalDateTime.now()));
        } catch (DuplicateKeyException alreadyGranted) {
            // 并发上报同时触发发放时，唯一键兜底，只发一次
            return null;
        }
        accountPointsService.addPoints(accountId, reward);
        return reward;
    }

    private Map<Integer, List<CourseChapter>> chaptersGroupedByCourse() {
        return chapterMapper.selectList(new LambdaQueryWrapper<CourseChapter>()
                        .orderByAsc(CourseChapter::getSortOrder).orderByAsc(CourseChapter::getId))
                .stream().collect(Collectors.groupingBy(CourseChapter::getCourseId));
    }

    private CourseProgressVO toVO(Course course, List<CourseChapter> chapters, List<CourseProgress> rows) {
        HashSet<Integer> inCourse = chapters.stream().map(CourseChapter::getId)
                .collect(Collectors.toCollection(HashSet::new));
        List<CourseProgress> valid = rows.stream()
                .filter(row -> inCourse.contains(row.getChapterId())).toList();
        int studied = valid.size();
        int total = chapters.size();
        LocalDateTime lastStudiedAt = valid.stream().map(CourseProgress::getUpdatedAt)
                .max(LocalDateTime::compareTo).orElse(null);
        String status = statusOf(studied, total);
        return new CourseProgressVO(course.getId(), course.getTitle(), course.getDescription(), course.getIcon(),
                total, studied, status,
                valid.stream().mapToInt(CourseProgress::getDurationSeconds).sum(),
                lastStudiedAt,
                STATUS_COMPLETED.equals(status) ? lastStudiedAt : null);
    }

    private String statusOf(int studied, int total) {
        if (studied == 0 || total == 0) {
            return STATUS_NOT_STARTED;
        }
        return studied >= total ? STATUS_COMPLETED : STATUS_IN_PROGRESS;
    }
}
