package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tick.entity.dto.CourseChapter;
import com.tick.entity.dto.CourseProgress;
import com.tick.entity.dto.GroupMemberVisit;
import com.tick.entity.dto.GroupTaskItem;
import com.tick.entity.dto.GroupTaskProgress;
import com.tick.entity.dto.ResourceAccessLog;
import com.tick.entity.dto.StudyGroupMember;
import com.tick.entity.dto.StudyGroupMessage;
import com.tick.mapper.CourseChapterMapper;
import com.tick.mapper.CourseProgressMapper;
import com.tick.mapper.GroupMemberVisitMapper;
import com.tick.mapper.GroupTaskItemMapper;
import com.tick.mapper.GroupTaskProgressMapper;
import com.tick.mapper.ResourceAccessLogMapper;
import com.tick.service.impl.group.GroupTaskCompletionContext;
import com.tick.service.impl.group.GroupTaskItemJudge;
import com.tick.service.impl.group.GroupTaskItemState;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学习任务完成判定：批量加载判定上下文，再按任务项类型交给对应的 GroupTaskItemJudge。
 */
@Component
public class GroupTaskCompletionService {
    private final GroupTaskItemMapper taskItemMapper;
    private final GroupTaskProgressMapper taskProgressMapper;
    private final GroupMemberVisitMapper visitMapper;
    private final ResourceAccessLogMapper accessLogMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseProgressMapper courseProgressMapper;
    private final Map<String, GroupTaskItemJudge> judges;

    public GroupTaskCompletionService(GroupTaskItemMapper taskItemMapper,
                                      GroupTaskProgressMapper taskProgressMapper,
                                      GroupMemberVisitMapper visitMapper,
                                      ResourceAccessLogMapper accessLogMapper,
                                      CourseChapterMapper chapterMapper,
                                      CourseProgressMapper courseProgressMapper,
                                      List<GroupTaskItemJudge> judges) {
        this.taskItemMapper = taskItemMapper;
        this.taskProgressMapper = taskProgressMapper;
        this.visitMapper = visitMapper;
        this.accessLogMapper = accessLogMapper;
        this.chapterMapper = chapterMapper;
        this.courseProgressMapper = courseProgressMapper;
        this.judges = judges.stream()
                .collect(Collectors.toUnmodifiableMap(GroupTaskItemJudge::type, Function.identity()));
    }

    public GroupTaskItemState state(GroupTaskCompletionContext context, GroupTaskItem item,
                                    Integer accountId, LocalDateTime taskCreatedAt) {
        GroupTaskItemJudge judge = judges.get(item.getItemType());
        if (judge == null) {
            return new GroupTaskItemState(false, null);
        }
        return judge.judge(context, item, accountId, taskCreatedAt);
    }

    /**
     * 批量加载一组任务的完成判定上下文，避免按项逐次查库。
     * 只统计本组成员、且只读取任务发布之后的资源访问流水，避免随流水表增长而全量扫描。
     */
    public GroupTaskCompletionContext loadContext(Integer groupId, List<StudyGroupMessage> tasks,
                                                  List<StudyGroupMember> members) {
        List<Integer> messageIds = tasks.stream().map(StudyGroupMessage::getId).toList();
        List<GroupTaskItem> items = taskItemMapper.selectList(
                new LambdaQueryWrapper<GroupTaskItem>()
                        .in(GroupTaskItem::getMessageId, messageIds)
                        .orderByAsc(GroupTaskItem::getSortOrder)
                        .orderByAsc(GroupTaskItem::getId));
        Map<Integer, List<GroupTaskItem>> itemsByMessage = items.stream()
                .collect(Collectors.groupingBy(GroupTaskItem::getMessageId));

        Map<String, LocalDateTime> manual = new HashMap<>();
        List<Integer> itemIds = items.stream().map(GroupTaskItem::getId).toList();
        if (!itemIds.isEmpty()) {
            for (GroupTaskProgress row : taskProgressMapper.selectList(
                    new LambdaQueryWrapper<GroupTaskProgress>()
                            .in(GroupTaskProgress::getItemId, itemIds))) {
                manual.put(row.getItemId() + "_" + row.getAccountId(), row.getCompletedAt());
            }
        }

        List<Integer> memberIds = members.stream().map(StudyGroupMember::getAccountId).toList();
        LocalDateTime taskWindowStart = tasks.stream()
                .map(StudyGroupMessage::getCreatedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        Set<Integer> courseIds = items.stream()
                .filter(item -> GroupTaskItem.TYPE_COURSE.equals(item.getItemType()))
                .map(GroupTaskItem::getRefId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Long> chapterCounts = new HashMap<>();
        Map<String, GroupTaskCompletionContext.CourseStudy> studies = new HashMap<>();
        if (!courseIds.isEmpty()) {
            for (CourseChapter chapter : chapterMapper.selectList(
                    new LambdaQueryWrapper<CourseChapter>()
                            .select(CourseChapter::getCourseId)
                            .in(CourseChapter::getCourseId, courseIds))) {
                chapterCounts.merge(chapter.getCourseId(), 1L, Long::sum);
            }
            if (!memberIds.isEmpty()) {
                for (CourseProgress row : courseProgressMapper.selectList(
                        new LambdaQueryWrapper<CourseProgress>()
                                .select(CourseProgress::getAccountId, CourseProgress::getCourseId,
                                        CourseProgress::getUpdatedAt)
                                .in(CourseProgress::getCourseId, courseIds)
                                .in(CourseProgress::getAccountId, memberIds))) {
                    studies.compute(row.getAccountId() + "_" + row.getCourseId(), (key, study) -> {
                        long count = study == null ? 1 : study.count() + 1;
                        LocalDateTime lastAt = study == null ? null : study.lastAt();
                        if (lastAt == null || (row.getUpdatedAt() != null
                                && row.getUpdatedAt().isAfter(lastAt))) {
                            lastAt = row.getUpdatedAt();
                        }
                        return new GroupTaskCompletionContext.CourseStudy(count, lastAt);
                    });
                }
            }
        }

        Set<Integer> resourceIds = items.stream()
                .filter(item -> GroupTaskItem.TYPE_RESOURCE.equals(item.getItemType()))
                .map(GroupTaskItem::getRefId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, LocalDateTime> accesses = new HashMap<>();
        if (!resourceIds.isEmpty() && !memberIds.isEmpty()) {
            for (ResourceAccessLog log : accessLogMapper.selectList(
                    new LambdaQueryWrapper<ResourceAccessLog>()
                            .select(ResourceAccessLog::getAccountId, ResourceAccessLog::getResourceId,
                                    ResourceAccessLog::getAccessedAt)
                            .in(ResourceAccessLog::getResourceId, resourceIds)
                            .in(ResourceAccessLog::getAccountId, memberIds)
                            .ge(taskWindowStart != null, ResourceAccessLog::getAccessedAt, taskWindowStart))) {
                accesses.merge(log.getAccountId() + "_" + log.getResourceId(), log.getAccessedAt(),
                        (a, b) -> a.isAfter(b) ? a : b);
            }
        }

        Map<Integer, LocalDateTime> visits = new HashMap<>();
        for (GroupMemberVisit visit : visitMapper.selectList(
                new LambdaQueryWrapper<GroupMemberVisit>()
                        .eq(GroupMemberVisit::getGroupId, groupId))) {
            visits.put(visit.getAccountId(), visit.getLastVisitAt());
        }
        return new GroupTaskCompletionContext(itemsByMessage, manual, chapterCounts, studies, accesses, visits);
    }
}