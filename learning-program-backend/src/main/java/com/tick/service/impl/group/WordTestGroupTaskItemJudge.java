package com.tick.service.impl.group;

import com.tick.entity.dto.GroupTaskItem;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 单词测试任务项：任务发布后的指定词表成绩达到及格分即完成。
 */
@Component
public class WordTestGroupTaskItemJudge implements GroupTaskItemJudge {
    @Override
    public String type() {
        return GroupTaskItem.TYPE_WORD_TEST;
    }

    @Override
    public GroupTaskItemState judge(GroupTaskCompletionContext context, GroupTaskItem item,
                                    Integer accountId, LocalDateTime taskCreatedAt) {
        int passScore = item.getPassScore() == null ? 0 : item.getPassScore();
        LocalDateTime completedAt = context.wordTests(accountId, item.getSource()).stream()
                .filter(score -> score.score() >= passScore)
                .filter(score -> taskCreatedAt == null || !score.createdAt().isBefore(taskCreatedAt))
                .map(GroupTaskCompletionContext.TestScore::createdAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        return new GroupTaskItemState(completedAt != null, completedAt);
    }
}
