package com.tick.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GroupTaskSettlementServiceTest {
    private final StudyGroupMemberQuery memberQuery = mock(StudyGroupMemberQuery.class);
    private final StudyGroupMessageMapper messageMapper = mock(StudyGroupMessageMapper.class);
    private final GroupTaskFinishMapper finishMapper = mock(GroupTaskFinishMapper.class);
    private final StudyGroupMapper groupMapper = mock(StudyGroupMapper.class);
    private final GroupTaskCompletionService completionService = mock(GroupTaskCompletionService.class);
    private final AccountPointsService pointsService = mock(AccountPointsService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final LocalDateTime joinedAt = LocalDateTime.of(2026, 11, 1, 9, 0);
    private final StudyGroupMember membership = new StudyGroupMember();
    private final StudyGroupMessage task = new StudyGroupMessage();
    private final GroupTaskItem item = new GroupTaskItem();
    private final GroupTaskCompletionContext context = mock(GroupTaskCompletionContext.class);

    private GroupTaskSettlementService service() {
        return new GroupTaskSettlementService(memberQuery, messageMapper, finishMapper, groupMapper,
                completionService, pointsService, notificationService);
    }

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, StudyGroupMessage.class);
    }

    @BeforeEach
    void setUp() {
        membership.setGroupId(10);
        membership.setAccountId(2);
        membership.setJoinedAt(joinedAt);
        task.setId(5);
        task.setGroupId(10);
        task.setType(StudyGroupMessage.TYPE_TASK);
        task.setTitle("本周任务");
        task.setCreatedAt(joinedAt.plusHours(1));
        item.setId(7);
        when(memberQuery.membershipsOf(2)).thenReturn(List.of(membership));
        when(messageMapper.selectList(any())).thenReturn(List.of(task));
        when(memberQuery.membersOf(10)).thenReturn(List.of(membership));
        when(completionService.loadContext(10, List.of(task), List.of(membership))).thenReturn(context);
        when(context.items(5)).thenReturn(List.of(item));
        StudyGroup group = new StudyGroup();
        group.setId(10);
        group.setOwnerId(1);
        group.setName("测试小组");
        when(groupMapper.selectById(10)).thenReturn(group);
    }

    @Test
    void settlementOnlySelectsTasksPublishedAfterJoining() {
        when(completionService.state(context, item, 2, task.getCreatedAt(), joinedAt))
                .thenReturn(new GroupTaskItemState(false, null));

        service().settleForAccount(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<StudyGroupMessage>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(messageMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment();
        assertTrue(sql.contains("created_at >= "), sql);
        assertFalse(sql.contains("created_at <= "), sql);
        @SuppressWarnings("unchecked")
        LambdaQueryWrapper<StudyGroupMessage> wrapper = (LambdaQueryWrapper<StudyGroupMessage>) captor.getValue();
        assertTrue(wrapper.getParamNameValuePairs().containsValue(joinedAt));
    }

    @Test
    void completedTaskAwardsPointsAndNotifiesOwner() {
        when(completionService.state(context, item, 2, task.getCreatedAt(), joinedAt))
                .thenReturn(new GroupTaskItemState(true, joinedAt.plusHours(2)));

        service().settleForAccount(2);

        verify(finishMapper).insert(any(GroupTaskFinish.class));
        verify(pointsService).addPoints(2, 1);
        verify(notificationService).create(1, "小组成员完成了学习任务",
                "测试小组：本周任务，获得 1 积分", "/groups/10", "group_task_finished");
    }

    @Test
    void incompleteTaskDoesNotSettle() {
        when(completionService.state(context, item, 2, task.getCreatedAt(), joinedAt))
                .thenReturn(new GroupTaskItemState(false, null));

        service().settleForAccount(2);

        verify(finishMapper, never()).insert(any(GroupTaskFinish.class));
        verifyNoInteractions(pointsService, notificationService);
    }

    @Test
    void duplicateSettlementDoesNotAwardAgain() {
        when(completionService.state(context, item, 2, task.getCreatedAt(), joinedAt))
                .thenReturn(new GroupTaskItemState(true, joinedAt.plusHours(2)));
        doThrow(new DuplicateKeyException("duplicate")).when(finishMapper).insert(any(GroupTaskFinish.class));

        service().settleForAccount(2);

        verifyNoInteractions(pointsService, notificationService);
    }
}
