package com.tick.service.impl;

import com.tick.entity.dto.GroupTaskFinish;
import com.tick.entity.dto.GroupTaskItem;
import com.tick.entity.dto.StudyGroup;
import com.tick.entity.dto.StudyGroupMember;
import com.tick.entity.dto.StudyGroupMessage;
import com.tick.entity.vo.response.GroupOverviewVO;
import com.tick.entity.vo.response.GroupTodoVO;
import com.tick.mapper.GroupTaskFinishMapper;
import com.tick.mapper.StudyGroupMapper;
import com.tick.mapper.StudyGroupMessageMapper;
import com.tick.service.impl.group.GroupTaskCompletionContext;
import com.tick.service.impl.group.GroupTaskItemState;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupOverviewServiceImplTest {
    private final StudyGroupMemberQuery memberQuery = mock(StudyGroupMemberQuery.class);
    private final StudyGroupMapper groupMapper = mock(StudyGroupMapper.class);
    private final StudyGroupMessageMapper messageMapper = mock(StudyGroupMessageMapper.class);
    private final GroupTaskFinishMapper finishMapper = mock(GroupTaskFinishMapper.class);
    private final GroupTaskCompletionService completionService = mock(GroupTaskCompletionService.class);
    private final GroupOverviewServiceImpl service = new GroupOverviewServiceImpl(memberQuery, groupMapper,
            messageMapper, finishMapper, completionService);

    @BeforeAll
    static void initTableMetadata() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, StudyGroupMessage.class);
        TableInfoHelper.initTableInfo(assistant, GroupTaskFinish.class);
    }

    @Test
    void emptyAccountGetsEmptyOverview() {
        when(memberQuery.membershipsOf(2)).thenReturn(List.of());

        GroupOverviewVO overview = service.getOverview(2);

        assertTrue(overview.getGroups().isEmpty());
        assertTrue(overview.getTodos().isEmpty());
        assertEquals(0, overview.getTotalOngoing());
    }

    @Test
    void overviewReturnsGroupMetricsAndCurrentUserTodos() {
        LocalDateTime joinedAt = LocalDateTime.of(2026, 11, 1, 9, 0);
        StudyGroupMember membership = new StudyGroupMember();
        membership.setGroupId(10);
        membership.setAccountId(2);
        membership.setRole(StudyGroupMember.ROLE_MEMBER);
        membership.setJoinedAt(joinedAt);
        StudyGroup group = new StudyGroup();
        group.setId(10);
        group.setName("测试小组");
        group.setOwnerId(1);
        group.setCreatedAt(joinedAt.minusDays(1));
        StudyGroupMessage task = new StudyGroupMessage();
        task.setId(5);
        task.setGroupId(10);
        task.setType(StudyGroupMessage.TYPE_TASK);
        task.setTitle("本周任务");
        task.setCreatedAt(joinedAt.plusHours(1));
        task.setDueAt(joinedAt.plusDays(1));
        GroupTaskItem item = new GroupTaskItem();
        item.setId(7);
        item.setTitle("阅读资料");
        GroupTaskCompletionContext context = mock(GroupTaskCompletionContext.class);
        when(memberQuery.membershipsOf(2)).thenReturn(List.of(membership));
        when(groupMapper.selectBatchIds(any())).thenReturn(List.of(group));
        when(memberQuery.memberCounts(any())).thenReturn(Map.of(10, 3L));
        when(memberQuery.usernames(any())).thenReturn(Map.of(1, "组长"));
        when(messageMapper.selectList(any())).thenReturn(List.of(task));
        when(finishMapper.selectList(any())).thenReturn(List.<GroupTaskFinish>of());
        when(memberQuery.membersOf(10)).thenReturn(List.of(membership));
        when(completionService.loadContext(10, List.of(task), List.of(membership))).thenReturn(context);
        when(context.items(5)).thenReturn(List.of(item));
        when(completionService.state(context, item, 2, task.getCreatedAt(), joinedAt))
                .thenReturn(new GroupTaskItemState(false, null));

        GroupOverviewVO overview = service.getOverview(2);

        assertEquals(1, overview.getGroups().size());
        assertEquals(3, overview.getGroups().getFirst().getMemberCount());
        assertEquals(1, overview.getTotalOngoing());
        assertEquals(1, overview.getTotalOverdue());
        assertEquals(1, overview.getTodos().size());
        GroupTodoVO todo = overview.getTodos().getFirst();
        assertEquals("阅读资料", todo.getItemTitle());
        assertEquals(task.getDueAt(), todo.getDueAt());
    }
}
