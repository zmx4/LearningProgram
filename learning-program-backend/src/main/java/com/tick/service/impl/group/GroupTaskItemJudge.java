package com.tick.service.impl.group;

import com.tick.entity.dto.GroupTaskItem;

import java.time.LocalDateTime;

/**
 * 单个任务项类型的完成判定策略。
 * 每个 item_type 对应一个实现，由 GroupTaskCompletionService 按类型分发。
 */
public interface GroupTaskItemJudge {
    String type();

    GroupTaskItemState judge(GroupTaskCompletionContext context, GroupTaskItem item,
                             Integer accountId, LocalDateTime taskCreatedAt);
}