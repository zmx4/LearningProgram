package com.tick.service.impl.group;

import com.tick.entity.dto.GroupTaskItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 组员访问任务项：任务发布后访问过小组详情页即完成。
 */
@Component
public class MemberVisitGroupTaskItemJudge implements GroupTaskItemJudge {
    @Override
    public String type() {
        return GroupTaskItem.TYPE_MEMBER_VISIT;
    }

    @Override
    public GroupTaskItemState judge(GroupTaskCompletionContext context, GroupTaskItem item,
                                    Integer accountId, LocalDateTime taskCreatedAt) {
        LocalDateTime at = context.visitAt(accountId);
        boolean completed = at != null && (taskCreatedAt == null || !at.isBefore(taskCreatedAt));
        return new GroupTaskItemState(completed, completed ? at : null);
    }
}