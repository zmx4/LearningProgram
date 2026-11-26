package com.tick.service.impl.group;

import com.tick.entity.dto.GroupTaskItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 课程任务项：课程章节全部学完即完成。
 */
@Component
public class CourseGroupTaskItemJudge implements GroupTaskItemJudge {
    @Override
    public String type() {
        return GroupTaskItem.TYPE_COURSE;
    }

    @Override
    public GroupTaskItemState judge(GroupTaskCompletionContext context, GroupTaskItem item,
                                    Integer accountId, LocalDateTime taskCreatedAt) {
        long total = context.chapterCount(item.getRefId());
        GroupTaskCompletionContext.CourseStudy study = context.courseStudy(accountId, item.getRefId());
        boolean completed = total > 0 && study != null && study.count() >= total;
        return new GroupTaskItemState(completed, completed ? study.lastAt() : null);
    }
}