package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tick.entity.dto.GroupTaskFinish;
import com.tick.entity.dto.GroupTaskItem;
import com.tick.entity.dto.StudyGroup;
import com.tick.entity.dto.StudyGroupMember;
import com.tick.entity.dto.StudyGroupMessage;
import com.tick.entity.vo.response.GroupOverviewItemVO;
import com.tick.entity.vo.response.GroupOverviewVO;
import com.tick.entity.vo.response.GroupTodoVO;
import com.tick.mapper.GroupTaskFinishMapper;
import com.tick.mapper.StudyGroupMapper;
import com.tick.mapper.StudyGroupMessageMapper;
import com.tick.service.GroupOverviewService;
import com.tick.service.impl.group.GroupTaskCompletionContext;
import com.tick.service.impl.group.GroupTaskItemState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 首页小组概览读模型：批量读取小组、任务和完成台账，不生成全成员进度矩阵。
 */
@Service
public class GroupOverviewServiceImpl implements GroupOverviewService {
    private final StudyGroupMemberQuery memberQuery;
    private final StudyGroupMapper groupMapper;
    private final StudyGroupMessageMapper messageMapper;
    private final GroupTaskFinishMapper finishMapper;
    private final GroupTaskCompletionService completionService;

    public GroupOverviewServiceImpl(StudyGroupMemberQuery memberQuery,
                                    StudyGroupMapper groupMapper,
                                    StudyGroupMessageMapper messageMapper,
                                    GroupTaskFinishMapper finishMapper,
                                    GroupTaskCompletionService completionService) {
        this.memberQuery = memberQuery;
        this.groupMapper = groupMapper;
        this.messageMapper = messageMapper;
        this.finishMapper = finishMapper;
        this.completionService = completionService;
    }

    @Override
    public GroupOverviewVO getOverview(Integer accountId) {
        List<StudyGroupMember> memberships = memberQuery.membershipsOf(accountId);
        if (memberships.isEmpty()) {
            return new GroupOverviewVO(List.of(), List.of(), 0, 0, 0);
        }
        Map<Integer, String> roleByGroup = memberships.stream()
                .collect(Collectors.toMap(StudyGroupMember::getGroupId, StudyGroupMember::getRole));
        List<StudyGroup> groups = groupMapper.selectBatchIds(roleByGroup.keySet()).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        if (groups.isEmpty()) {
            return new GroupOverviewVO(List.of(), List.of(), 0, 0, 0);
        }

        Map<Integer, Long> memberCounts = memberQuery.memberCounts(roleByGroup.keySet());
        Map<Integer, String> ownerNames = memberQuery.usernames(groups.stream().map(StudyGroup::getOwnerId).toList());
        Map<Integer, List<StudyGroupMessage>> tasksByGroup = messageMapper.selectList(
                        new LambdaQueryWrapper<StudyGroupMessage>()
                                .in(StudyGroupMessage::getGroupId, roleByGroup.keySet())
                                .eq(StudyGroupMessage::getType, StudyGroupMessage.TYPE_TASK)
                                .orderByAsc(StudyGroupMessage::getId))
                .stream().collect(Collectors.groupingBy(StudyGroupMessage::getGroupId));
        Set<Integer> taskIds = tasksByGroup.values().stream().flatMap(List::stream)
                .map(StudyGroupMessage::getId).collect(Collectors.toSet());
        Map<Integer, GroupTaskFinish> finishes = new HashMap<>();
        if (!taskIds.isEmpty()) {
            for (GroupTaskFinish finish : finishMapper.selectList(
                    new LambdaQueryWrapper<GroupTaskFinish>()
                            .in(GroupTaskFinish::getTaskId, taskIds)
                            .eq(GroupTaskFinish::getAccountId, accountId))) {
                finishes.put(finish.getTaskId(), finish);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<GroupOverviewItemVO> items = new ArrayList<>();
        List<GroupTodoVO> todos = new ArrayList<>();
        int totalOngoing = 0;
        int totalOverdue = 0;
        int totalCompleted = 0;
        for (StudyGroup group : groups) {
            StudyGroupMember membership = memberships.stream()
                    .filter(member -> member.getGroupId().equals(group.getId()))
                    .findFirst().orElseThrow();
            List<StudyGroupMessage> tasks = tasksByGroup.getOrDefault(group.getId(), List.of());
            List<StudyGroupMember> members = memberQuery.membersOf(group.getId());
            GroupTaskCompletionContext context = tasks.isEmpty()
                    ? GroupTaskCompletionContext.empty()
                    : completionService.loadContext(group.getId(), tasks, members);
            int ongoing = 0;
            int overdue = 0;
            int completed = 0;
            for (StudyGroupMessage task : tasks) {
                if (finishes.containsKey(task.getId())) {
                    completed++;
                    continue;
                }
                List<GroupTaskItem> taskItems = context.items(task.getId());
                if (taskItems.isEmpty()) {
                    continue;
                }
                boolean allCompleted = true;
                for (GroupTaskItem taskItem : taskItems) {
                    GroupTaskItemState state = completionService.state(context, taskItem, accountId,
                            task.getCreatedAt(), membership.getJoinedAt());
                    if (state.completed()) {
                        continue;
                    }
                    allCompleted = false;
                    todos.add(new GroupTodoVO(group.getId() + "-" + task.getId() + "-" + taskItem.getId(),
                            group.getId(), group.getName(), task.getId(), task.getTitle(),
                            taskItem.getId(), taskItem.getTitle(), task.getDueAt()));
                }
                if (allCompleted) {
                    completed++;
                } else {
                    ongoing++;
                    if (task.getDueAt() != null && task.getDueAt().isBefore(now)) {
                        overdue++;
                    }
                }
            }
            totalOngoing += ongoing;
            totalOverdue += overdue;
            totalCompleted += completed;
            items.add(new GroupOverviewItemVO(group.getId(), group.getName(), group.getOwnerId(),
                    ownerNames.get(group.getOwnerId()), group.getInviteCode(),
                    memberCounts.getOrDefault(group.getId(), 0L).intValue(),
                    roleByGroup.get(group.getId()), group.getCreatedAt(), ongoing, overdue, completed));
        }
        todos.sort(Comparator
                .comparing(GroupTodoVO::getDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(GroupTodoVO::getTaskId)
                .thenComparing(GroupTodoVO::getItemId));
        return new GroupOverviewVO(items, todos, totalOngoing, totalOverdue, totalCompleted);
    }
}
