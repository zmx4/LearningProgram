package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tick.entity.dto.GroupTaskFinish;
import com.tick.entity.dto.GroupTaskItem;
import com.tick.entity.dto.StudyGroup;
import com.tick.entity.dto.StudyGroupMember;
import com.tick.entity.dto.StudyGroupMessage;
import com.tick.mapper.GroupTaskFinishMapper;
import com.tick.mapper.StudyGroupMapper;
import com.tick.mapper.StudyGroupMessageMapper;
import com.tick.service.AccountPointsService;
import com.tick.service.NotificationService;
import com.tick.service.impl.group.GroupTaskCompletionContext;
import com.tick.service.impl.group.GroupTaskItemState;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小组任务结算：只在学习写事务提交后运行，完成台账唯一键保证积分与通知只发一次。
 */
@Service
public class GroupTaskSettlementService {
    private static final int POINTS_PER_ITEM = 1;

    private final StudyGroupMemberQuery memberQuery;
    private final StudyGroupMessageMapper messageMapper;
    private final GroupTaskFinishMapper finishMapper;
    private final StudyGroupMapper groupMapper;
    private final GroupTaskCompletionService completionService;
    private final AccountPointsService accountPointsService;
    private final NotificationService notificationService;

    public GroupTaskSettlementService(StudyGroupMemberQuery memberQuery,
                                      StudyGroupMessageMapper messageMapper,
                                      GroupTaskFinishMapper finishMapper,
                                      StudyGroupMapper groupMapper,
                                      GroupTaskCompletionService completionService,
                                      AccountPointsService accountPointsService,
                                      NotificationService notificationService) {
        this.memberQuery = memberQuery;
        this.messageMapper = messageMapper;
        this.finishMapper = finishMapper;
        this.groupMapper = groupMapper;
        this.completionService = completionService;
        this.accountPointsService = accountPointsService;
        this.notificationService = notificationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void settleForAccount(Integer accountId) {
        List<StudyGroupMember> memberships = memberQuery.membershipsOf(accountId);
        for (StudyGroupMember membership : memberships) {
            List<StudyGroupMessage> tasks = messageMapper.selectList(
                    new LambdaQueryWrapper<StudyGroupMessage>()
                            .eq(StudyGroupMessage::getGroupId, membership.getGroupId())
                            .eq(StudyGroupMessage::getType, StudyGroupMessage.TYPE_TASK)
                            .le(membership.getJoinedAt() != null, StudyGroupMessage::getCreatedAt,
                                    membership.getJoinedAt())
                            .orderByAsc(StudyGroupMessage::getId));
            if (tasks.isEmpty()) {
                continue;
            }
            List<StudyGroupMember> members = memberQuery.membersOf(membership.getGroupId());
            GroupTaskCompletionContext context = completionService.loadContext(membership.getGroupId(), tasks, members);
            StudyGroup group = groupMapper.selectById(membership.getGroupId());
            for (StudyGroupMessage task : tasks) {
                List<GroupTaskItem> items = context.items(task.getId());
                if (items.isEmpty()) {
                    continue;
                }
                LocalDateTime completedAt = null;
                boolean allCompleted = true;
                for (GroupTaskItem item : items) {
                    GroupTaskItemState state = completionService.state(context, item, accountId,
                            task.getCreatedAt(), membership.getJoinedAt());
                    if (!state.completed()) {
                        allCompleted = false;
                        break;
                    }
                    if (state.completedAt() != null && (completedAt == null || state.completedAt().isAfter(completedAt))) {
                        completedAt = state.completedAt();
                    }
                }
                if (!allCompleted) {
                    continue;
                }
                int points = items.size() * POINTS_PER_ITEM;
                GroupTaskFinish finish = new GroupTaskFinish();
                finish.setGroupId(membership.getGroupId());
                finish.setTaskId(task.getId());
                finish.setAccountId(accountId);
                finish.setCompletedAt(completedAt == null ? LocalDateTime.now() : completedAt);
                finish.setPointsAwarded(points);
                try {
                    finishMapper.insert(finish);
                } catch (DuplicateKeyException ignored) {
                    continue;
                }
                accountPointsService.addPoints(accountId, points);
                if (group != null && !accountId.equals(group.getOwnerId())) {
                    notificationService.create(group.getOwnerId(), "小组成员完成了学习任务",
                            (group.getName() + "：" + task.getTitle() + "，获得 " + points + " 积分"),
                            "/groups/" + group.getId(), "group_task_finished");
                }
            }
        }
    }
}
