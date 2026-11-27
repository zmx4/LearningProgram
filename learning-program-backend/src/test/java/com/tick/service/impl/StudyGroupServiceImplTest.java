package com.tick.service.impl;

import com.tick.entity.dto.StudyGroup;
import com.tick.entity.dto.StudyGroupMember;
import com.tick.entity.vo.request.StudyGroupJoinVO;
import com.tick.exception.NotFoundException;
import com.tick.mapper.GroupMemberVisitMapper;
import com.tick.mapper.StudyGroupMapper;
import com.tick.mapper.StudyGroupMemberMapper;
import com.tick.mapper.StudyGroupMessageMapper;
import com.tick.service.AccountService;
import com.tick.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

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
        return service(new StudyGroupMemberQuery(memberMapper, mock(AccountService.class)));
    }

    private StudyGroupServiceImpl service(StudyGroupMemberQuery memberQuery) {
        return new StudyGroupServiceImpl(
                memberMapper,
                mock(StudyGroupMessageMapper.class),
                mock(GroupMemberVisitMapper.class),
                memberQuery,
                mock(NotificationService.class),
                mock(ApplicationEventPublisher.class));
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
    void concurrentJoinConflictReturnsFriendlyMessage() {
        StudyGroupMapper groupMapper = mock(StudyGroupMapper.class);
        StudyGroupMemberQuery memberQuery = mock(StudyGroupMemberQuery.class);
        StudyGroupServiceImpl service = service(memberQuery);
        ReflectionTestUtils.setField(service, "baseMapper", groupMapper);

        StudyGroup group = new StudyGroup();
        group.setId(10);
        group.setName("重构测试小组");
        group.setOwnerId(7);
        group.setInviteCode("ABCDEFGH");
        when(groupMapper.selectOne(any())).thenReturn(group);
        when(memberQuery.membership(10, 1)).thenReturn(null);
        when(memberMapper.insert(any(StudyGroupMember.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));
        StudyGroupJoinVO vo = new StudyGroupJoinVO();
        vo.setInviteCode("abcdefgh");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.joinByCode(1, vo));

        assertEquals("你已经在该小组中", exception.getMessage());
        verify(memberMapper).insert(any(StudyGroupMember.class));
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
