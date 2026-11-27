package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tick.entity.dto.Account;
import com.tick.entity.dto.StudyGroupMember;
import com.tick.mapper.StudyGroupMemberMapper;
import com.tick.service.AccountService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 小组成员查询：成员关系、成员列表和用户名的批量获取，供小组与任务服务共用。
 */
@Component
public class StudyGroupMemberQuery {
    private final StudyGroupMemberMapper memberMapper;
    private final AccountService accountService;

    public StudyGroupMemberQuery(StudyGroupMemberMapper memberMapper, AccountService accountService) {
        this.memberMapper = memberMapper;
        this.accountService = accountService;
    }

    public StudyGroupMember membership(Integer groupId, Integer accountId) {
        return memberMapper.selectOne(new LambdaQueryWrapper<StudyGroupMember>()
                .eq(StudyGroupMember::getGroupId, groupId)
                .eq(StudyGroupMember::getAccountId, accountId));
    }

    public List<StudyGroupMember> membershipsOf(Integer accountId) {
        return memberMapper.selectList(new LambdaQueryWrapper<StudyGroupMember>()
                .eq(StudyGroupMember::getAccountId, accountId));
    }

    public List<StudyGroupMember> membersOf(Integer groupId) {
        return memberMapper.selectList(new LambdaQueryWrapper<StudyGroupMember>()
                .eq(StudyGroupMember::getGroupId, groupId)
                .orderByAsc(StudyGroupMember::getJoinedAt));
    }

    public Map<Integer, String> memberRoles(Integer groupId) {
        return memberMapper.selectList(new LambdaQueryWrapper<StudyGroupMember>()
                        .eq(StudyGroupMember::getGroupId, groupId))
                .stream()
                .collect(Collectors.toMap(StudyGroupMember::getAccountId, StudyGroupMember::getRole,
                        (a, b) -> a));
    }

    public Map<Integer, Long> memberCounts(Collection<Integer> groupIds) {
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, Long> counts = new HashMap<>();
        for (StudyGroupMember member : memberMapper.selectList(
                new LambdaQueryWrapper<StudyGroupMember>()
                        .in(StudyGroupMember::getGroupId, groupIds))) {
            counts.merge(member.getGroupId(), 1L, Long::sum);
        }
        return counts;
    }

    public long memberCount(Integer groupId) {
        return memberMapper.selectCount(new LambdaQueryWrapper<StudyGroupMember>()
                .eq(StudyGroupMember::getGroupId, groupId));
    }

    public Map<Integer, String> usernames(Collection<Integer> accountIds) {
        Set<Integer> ids = accountIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Integer, String> names = new HashMap<>();
        for (Account account : accountService.listByIds(ids)) {
            names.put(account.getId(), account.getUsername());
        }
        return names;
    }
}