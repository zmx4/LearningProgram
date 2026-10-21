package com.tick.service.impl;

import com.tick.entity.dto.StudyGroupMember;
import com.tick.exception.NotFoundException;
import com.tick.mapper.CourseChapterMapper;
import com.tick.mapper.CourseMapper;
import com.tick.mapper.CourseProgressMapper;
import com.tick.mapper.GroupMemberVisitMapper;
import com.tick.mapper.GroupTaskItemMapper;
import com.tick.mapper.GroupTaskProgressMapper;
import com.tick.mapper.LearningResourceMapper;
import com.tick.mapper.ResourceAccessLogMapper;
import com.tick.mapper.StudyGroupMemberMapper;
import com.tick.mapper.StudyGroupMessageMapper;
import com.tick.service.AccountService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 移除组员必须区分「小组/成员不存在」与「没有权限」，否则前端会把 404 场景当成越权。
 */
class StudyGroupServiceImplTest {
    private final StudyGroupMemberMapper memberMapper = mock(StudyGroupMemberMapper.class);

    private StudyGroupServiceImpl service() {
        return new StudyGroupServiceImpl(
                memberMapper,
                mock(StudyGroupMessageMapper.class),
                mock(GroupTaskItemMapper.class),
                mock(GroupTaskProgressMapper.class),
                mock(GroupMemberVisitMapper.class),
                mock(ResourceAccessLogMapper.class),
                mock(CourseMapper.class),
                mock(CourseChapterMapper.class),
                mock(CourseProgressMapper.class),
                mock(LearningResourceMapper.class),
                mock(AccountService.class));
    }

    private StudyGroupMember member(int id, int accountId, String role) {
        StudyGroupMember member = new StudyGroupMember();
        member.setId(id);
        member.setGroupId(10);
        member.setAccountId(accountId);
        member.setRole(role);
        return member;
    }

    @Test
    void nonMemberGetsNotFoundInsteadOfForbidden() {
        when(memberMapper.selectOne(any())).thenReturn(null);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> service().removeMember(1, 10, 2));

        assertEquals("小组不存在或你不是小组成员", exception.getMessage());
    }

    @Test
    void memberWithoutOwnerRoleGetsForbidden() {
        when(memberMapper.selectOne(any()))
                .thenReturn(member(11, 1, StudyGroupMember.ROLE_MEMBER));

        SecurityException exception = assertThrows(SecurityException.class,
                () -> service().removeMember(1, 10, 2));

        assertEquals("只有组长可以移除组员", exception.getMessage());
    }

    @Test
    void ownerCannotRemoveSelf() {
        when(memberMapper.selectOne(any()))
                .thenReturn(member(11, 1, StudyGroupMember.ROLE_OWNER));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service().removeMember(1, 10, 1));

        assertEquals("不能移除自己", exception.getMessage());
    }

    @Test
    void missingTargetGetsNotFound() {
        when(memberMapper.selectOne(any()))
                .thenReturn(member(11, 1, StudyGroupMember.ROLE_OWNER))
                .thenReturn(null);

        assertThrows(NotFoundException.class, () -> service().removeMember(1, 10, 2));
    }

    @Test
    void ownerCanRemoveGroupMember() {
        StudyGroupMember target = member(12, 2, StudyGroupMember.ROLE_MEMBER);
        when(memberMapper.selectOne(any()))
                .thenReturn(member(11, 1, StudyGroupMember.ROLE_OWNER))
                .thenReturn(target);

        service().removeMember(1, 10, 2);

        verify(memberMapper).deleteById(12);
    }
}
