package com.tick.service.impl.group;

import com.tick.entity.dto.GroupTaskItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 一组任务的完成判定上下文（批量预加载），避免按成员或任务项逐次查库。
 */
public final class GroupTaskCompletionContext {
    private final Map<Integer, List<GroupTaskItem>> itemsByMessage;
    private final Map<String, LocalDateTime> manual;
    private final Map<Integer, Long> chapterCounts;
    private final Map<String, CourseStudy> studies;
    private final Map<String, LocalDateTime> accesses;
    private final Map<Integer, LocalDateTime> visits;

    public GroupTaskCompletionContext(Map<Integer, List<GroupTaskItem>> itemsByMessage,
                                      Map<String, LocalDateTime> manual,
                                      Map<Integer, Long> chapterCounts,
                                      Map<String, CourseStudy> studies,
                                      Map<String, LocalDateTime> accesses,
                                      Map<Integer, LocalDateTime> visits) {
        this.itemsByMessage = itemsByMessage;
        this.manual = manual;
        this.chapterCounts = chapterCounts;
        this.studies = studies;
        this.accesses = accesses;
        this.visits = visits;
    }

    public static GroupTaskCompletionContext empty() {
        return new GroupTaskCompletionContext(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    public List<GroupTaskItem> items(Integer messageId) {
        return itemsByMessage.getOrDefault(messageId, List.of());
    }

    public LocalDateTime manualCompletedAt(Integer itemId, Integer accountId) {
        return manual.get(itemId + "_" + accountId);
    }

    public long chapterCount(Integer courseId) {
        return chapterCounts.getOrDefault(courseId, 0L);
    }

    public CourseStudy courseStudy(Integer accountId, Integer courseId) {
        return studies.get(accountId + "_" + courseId);
    }

    public LocalDateTime resourceAccessAt(Integer accountId, Integer resourceId) {
        return accesses.get(accountId + "_" + resourceId);
    }

    public LocalDateTime visitAt(Integer accountId) {
        return visits.get(accountId);
    }

    /** 某成员在某课程下已学章节数与最近学习时间。 */
    public record CourseStudy(long count, LocalDateTime lastAt) {
    }
}