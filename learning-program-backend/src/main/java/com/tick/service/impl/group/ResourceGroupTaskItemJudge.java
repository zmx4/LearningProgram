package com.tick.service.impl.group;

import com.tick.entity.dto.GroupTaskItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 资源任务项：任务发布后访问过该资源即完成。
 */
@Component
public class ResourceGroupTaskItemJudge implements GroupTaskItemJudge {
    @Override
    public String type() {
        return GroupTaskItem.TYPE_RESOURCE;
    }

    @Override
    public GroupTaskItemState judge(GroupTaskCompletionContext context, GroupTaskItem item,
                                    Integer accountId, LocalDateTime taskCreatedAt) {
        LocalDateTime at = context.resourceAccessAt(accountId, item.getRefId());
        boolean completed = at != null && (taskCreatedAt == null || !at.isBefore(taskCreatedAt));
        return new GroupTaskItemState(completed, completed ? at : null);
    }
}