package com.tick.service.impl.group;

import com.tick.entity.dto.GroupTaskItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 纯文本任务项：由组员手动标记完成，判定依据是 db_group_task_progress。
 */
@Component
public class TextGroupTaskItemJudge implements GroupTaskItemJudge {
    @Override
    public String type() {
        return GroupTaskItem.TYPE_TEXT;
    }

    @Override
    public GroupTaskItemState judge(GroupTaskCompletionContext context, GroupTaskItem item,
                                    Integer accountId, LocalDateTime taskCreatedAt) {
        LocalDateTime at = context.manualCompletedAt(item.getId(), accountId);
        return new GroupTaskItemState(at != null, at);
    }
}