package com.tick.service.impl;

import com.tick.entity.dto.GroupTaskItem;
import com.tick.entity.dto.GroupTaskProgress;
import com.tick.entity.dto.StudyGroupMember;
import com.tick.entity.dto.StudyGroupMessage;
import com.tick.entity.vo.request.GroupTaskCreateVO;
import com.tick.mapper.CourseMapper;
import com.tick.mapper.GroupTaskItemMapper;
import com.tick.mapper.GroupTaskProgressMapper;
import com.tick.mapper.LearningResourceMapper;
import com.tick.mapper.StudyGroupMessageMapper;
import com.tick.service.AccountService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupTaskServiceImplTest {
    private final StudyGroupMemberQuery memberQuery = mock(StudyGroupMemberQuery.class);
    private final StudyGroupMessageMapper messageMapper = mock(StudyGroupMessageMapper.class);
    private final GroupTaskItemMapper taskItemMapper = mock(GroupTaskItemMapper.class);
    private final GroupTaskProgressMapper taskProgressMapper = mock(GroupTaskProgressMapper.class);
    private final CourseMapper courseMapper = mock(CourseMapper.class);
    private final LearningResourceMapper resourceMapper = mock(LearningResourceMapper.class);
    private final AccountService accountService = mock(AccountService.class);
    private final GroupTaskCompletionService completionService = mock(GroupTaskCompletionService.class);

    private GroupTaskServiceImpl service() {
        return new GroupTaskServiceImpl(memberQuery, messageMapper, taskItemMapper, taskProgressMapper,
                courseMapper, resourceMapper, accountService, completionService);
    }

    private StudyGroupMember member(int accountId, String role) {
        StudyGroupMember member = new StudyGroupMember();
        member.setId(accountId + 100);
        member.setGroupId(10);
        member.setAccountId(accountId);
        member.setRole(role);
        return member;
    }

    @Test
    void nonMemberCannotPublishOrReadTasks() {
        when(memberQuery.membership(10, 1)).thenReturn(null);
        when(memberQuery.membersOf(10)).thenReturn(List.of());

        assertNull(service().postTask(1, 10, new GroupTaskCreateVO()));
        assertNull(service().listTasks(1, 10));
        assertNull(service().taskProgress(1, 10, 5));
    }

    @Test
    void onlyOwnerCanPublishTaskAndViewProgress() {
        when(memberQuery.membership(10, 1)).thenReturn(member(1, StudyGroupMember.ROLE_MEMBER));

        assertThrows(SecurityException.class,
                () -> service().postTask(1, 10, new GroupTaskCreateVO()));
        assertThrows(SecurityException.class,
                () -> service().taskProgress(1, 10, 5));
    }

    @Test
    void autoJudgedItemCannotBeCompletedManually() {
        when(memberQuery.membership(10, 1)).thenReturn(member(1, StudyGroupMember.ROLE_MEMBER));
        GroupTaskItem item = new GroupTaskItem();
        item.setId(3);
        item.setGroupId(10);
        item.setMessageId(5);
        item.setItemType(GroupTaskItem.TYPE_COURSE);
        when(taskItemMapper.selectById(3)).thenReturn(item);
        when(messageMapper.selectById(5)).thenReturn(taskMessage(5));

        assertThrows(IllegalArgumentException.class,
                () -> service().completeTaskItem(1, 10, 5, 3));
        verify(taskProgressMapper, never()).insert(any(GroupTaskProgress.class));
    }

    @Test
    void repeatedTextCompletionDoesNotInsertAgain() {
        when(memberQuery.membership(10, 1)).thenReturn(member(1, StudyGroupMember.ROLE_MEMBER));
        StudyGroupMember member = member(1, StudyGroupMember.ROLE_MEMBER);
        GroupTaskItem item = new GroupTaskItem();
        item.setId(3);
        item.setGroupId(10);
        item.setMessageId(5);
        item.setItemType(GroupTaskItem.TYPE_TEXT);
        when(taskItemMapper.selectById(3)).thenReturn(item);
        StudyGroupMessage task = taskMessage(5);
        when(messageMapper.selectById(5)).thenReturn(task);
        when(taskProgressMapper.selectCount(any())).thenReturn(1L);
        when(memberQuery.membersOf(10)).thenReturn(List.of(member));
        when(completionService.loadContext(10, List.of(task), List.of(member)))
                .thenReturn(com.tick.service.impl.group.GroupTaskCompletionContext.empty());

        service().completeTaskItem(1, 10, 5, 3);

        verify(taskProgressMapper, never()).insert(any(GroupTaskProgress.class));
    }

    private StudyGroupMessage taskMessage(int id) {
        StudyGroupMessage message = new StudyGroupMessage();
        message.setId(id);
        message.setGroupId(10);
        message.setAccountId(1);
        message.setType(StudyGroupMessage.TYPE_TASK);
        message.setTitle("任务");
        return message;
    }
}