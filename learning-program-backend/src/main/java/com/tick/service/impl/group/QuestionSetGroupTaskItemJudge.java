package com.tick.service.impl.group;

import com.tick.entity.dto.GroupTaskItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 题集测试任务项：任务发布后的该题集成绩达到及格分即完成。
 */
@Component
public class QuestionSetGroupTaskItemJudge implements GroupTaskItemJudge {
    @Override
    public String type() {
        return GroupTaskItem.TYPE_QUESTION_SET;
    }

    @Override
    public GroupTaskItemState judge(GroupTaskCompletionContext context, GroupTaskItem item,
                                    Integer accountId, LocalDateTime taskCreatedAt) {
        int passScore = item.getPassScore() == null ? 0 : item.getPassScore();
        LocalDateTime completedAt = context.questionSetTests(accountId, item.getRefId()).stream()
                .filter(score -> score.score() >= passScore)
                .filter(score -> taskCreatedAt == null || !score.createdAt().isBefore(taskCreatedAt))
                .map(GroupTaskCompletionContext.TestScore::createdAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        return new GroupTaskItemState(completedAt != null, completedAt);
    }
}
