package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Account;
import com.tick.entity.dto.StudyGroup;
import com.tick.entity.dto.StudyGroupMember;
import com.tick.entity.dto.StudyGroupMessage;
import com.tick.entity.vo.request.GroupMessageCreateVO;
import com.tick.entity.vo.request.GroupTaskCreateVO;
import com.tick.entity.vo.request.StudyGroupCreateVO;
import com.tick.entity.vo.request.StudyGroupJoinVO;
import com.tick.entity.vo.response.GroupMemberVO;
import com.tick.entity.vo.response.GroupMessageVO;
import com.tick.entity.vo.response.StudyGroupDetailVO;
import com.tick.entity.vo.response.StudyGroupVO;
import com.tick.mapper.StudyGroupMapper;
import com.tick.mapper.StudyGroupMemberMapper;
import com.tick.mapper.StudyGroupMessageMapper;
import com.tick.service.AccountService;
import com.tick.service.StudyGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学习小组服务实现。组长也占一行成员记录（role = owner），成员关系统一走 db_study_group_member；
 * 消息与成员的用户名批量换取，避免逐条查库。
 */
@Service
public class StudyGroupServiceImpl extends ServiceImpl<StudyGroupMapper, StudyGroup>
        implements StudyGroupService {
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MESSAGE_LIMIT = 200;
    private static final int INVITE_CODE_LENGTH = 8;
    /** 去除易混淆字符的邀请码字母表 */
    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StudyGroupMemberMapper memberMapper;
    private final StudyGroupMessageMapper messageMapper;
    private final AccountService accountService;

    public StudyGroupServiceImpl(StudyGroupMemberMapper memberMapper,
                                 StudyGroupMessageMapper messageMapper,
                                 AccountService accountService) {
        this.memberMapper = memberMapper;
        this.messageMapper = messageMapper;
        this.accountService = accountService;
    }

    @Override
    public List<StudyGroupVO> listMyGroups(Integer accountId) {
        List<StudyGroupMember> memberships = memberMapper.selectList(
                new LambdaQueryWrapper<StudyGroupMember>()
                        .eq(StudyGroupMember::getAccountId, accountId));
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> roleByGroup = memberships.stream()
                .collect(Collectors.toMap(StudyGroupMember::getGroupId, StudyGroupMember::getRole));
        List<StudyGroup> groups = listByIds(roleByGroup.keySet()).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
        Map<Integer, Long> memberCounts = memberCounts(roleByGroup.keySet());
        Map<Integer, String> names = usernames(groups.stream().map(StudyGroup::getOwnerId).toList());
        return groups.stream()
                .map(group -> toVO(group, roleByGroup.get(group.getId()),
                        names.get(group.getOwnerId()),
                        memberCounts.getOrDefault(group.getId(), 0L).intValue()))
                .toList();
    }

    @Override
    @Transactional
    public StudyGroupVO createGroup(Integer accountId, StudyGroupCreateVO vo) {
        String name = normalizeName(vo == null ? null : vo.getName());
        StudyGroup group = new StudyGroup();
        group.setName(name);
        group.setOwnerId(accountId);
        group.setInviteCode(generateInviteCode());
        group.setCreatedAt(LocalDateTime.now());
        save(group);

        StudyGroupMember owner = new StudyGroupMember();
        owner.setGroupId(group.getId());
        owner.setAccountId(accountId);
        owner.setRole(StudyGroupMember.ROLE_OWNER);
        owner.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(owner);

        Account account = accountService.getById(accountId);
        return toVO(group, StudyGroupMember.ROLE_OWNER,
                account == null ? null : account.getUsername(), 1);
    }

    @Override
    @Transactional
    public StudyGroupVO joinByCode(Integer accountId, StudyGroupJoinVO vo) {
        String code = vo == null || vo.getInviteCode() == null ? "" : vo.getInviteCode().trim().toUpperCase();
        if (code.isEmpty()) {
            throw new IllegalArgumentException("邀请码不能为空");
        }
        StudyGroup group = query().eq("invite_code", code).one();
        if (group == null) {
            throw new IllegalArgumentException("邀请码无效");
        }
        StudyGroupMember existing = membership(group.getId(), accountId);
        if (existing != null) {
            throw new IllegalArgumentException("你已经在该小组中");
        }
        StudyGroupMember member = new StudyGroupMember();
        member.setGroupId(group.getId());
        member.setAccountId(accountId);
        member.setRole(StudyGroupMember.ROLE_MEMBER);
        member.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(member);

        Account owner = accountService.getById(group.getOwnerId());
        long memberCount = memberMapper.selectCount(
                new LambdaQueryWrapper<StudyGroupMember>()
                        .eq(StudyGroupMember::getGroupId, group.getId()));
        return toVO(group, StudyGroupMember.ROLE_MEMBER,
                owner == null ? null : owner.getUsername(), (int) memberCount);
    }

    @Override
    public StudyGroupDetailVO getDetail(Integer accountId, Integer groupId) {
        StudyGroup group = getById(groupId);
        StudyGroupMember self = group == null ? null : membership(groupId, accountId);
        if (group == null || self == null) {
            return null;
        }
        List<StudyGroupMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<StudyGroupMember>()
                        .eq(StudyGroupMember::getGroupId, groupId)
                        .orderByAsc(StudyGroupMember::getJoinedAt));
        Map<Integer, String> names = usernames(members.stream().map(StudyGroupMember::getAccountId).toList());
        List<GroupMemberVO> memberVOs = members.stream()
                .map(member -> new GroupMemberVO(
                        member.getAccountId(),
                        names.get(member.getAccountId()),
                        member.getRole(),
                        member.getJoinedAt()))
                .toList();
        StudyGroupVO groupVO = toVO(group, self.getRole(), names.get(group.getOwnerId()), members.size());
        return new StudyGroupDetailVO(groupVO, memberVOs);
    }

    @Override
    public List<GroupMessageVO> listMessages(Integer accountId, Integer groupId) {
        if (membership(groupId, accountId) == null) {
            return null;
        }
        List<StudyGroupMessage> recent = messageMapper.selectList(
                new LambdaQueryWrapper<StudyGroupMessage>()
                        .eq(StudyGroupMessage::getGroupId, groupId)
                        .orderByDesc(StudyGroupMessage::getId)
                        .last("LIMIT " + MESSAGE_LIMIT));
        Collections.reverse(recent);
        Map<Integer, String> names = usernames(recent.stream().map(StudyGroupMessage::getAccountId).toList());
        Map<Integer, String> roles = memberRoles(groupId);
        return recent.stream()
                .map(message -> toMessageVO(message, names.get(message.getAccountId()),
                        roles.get(message.getAccountId())))
                .toList();
    }

    @Override
    public GroupMessageVO postMessage(Integer accountId, Integer groupId, GroupMessageCreateVO vo) {
        StudyGroupMember self = membership(groupId, accountId);
        if (self == null) {
            return null;
        }
        String content = normalizeContent(vo == null ? null : vo.getContent());
        StudyGroupMessage message = saveMessage(groupId, accountId, StudyGroupMessage.TYPE_TEXT, null, content);
        Account account = accountService.getById(accountId);
        return toMessageVO(message, account == null ? null : account.getUsername(), self.getRole());
    }

    @Override
    public GroupMessageVO postTask(Integer accountId, Integer groupId, GroupTaskCreateVO vo) {
        StudyGroupMember self = membership(groupId, accountId);
        if (self == null) {
            return null;
        }
        if (!StudyGroupMember.ROLE_OWNER.equals(self.getRole())) {
            throw new SecurityException("只有组长可以发布学习任务");
        }
        String title = vo == null || vo.getTitle() == null ? "" : vo.getTitle().trim();
        if (title.isEmpty() || title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("任务标题不能为空且不超过 " + MAX_TITLE_LENGTH + " 字");
        }
        String content = normalizeContent(vo.getContent());
        StudyGroupMessage message = saveMessage(groupId, accountId, StudyGroupMessage.TYPE_TASK, title, content);
        Account account = accountService.getById(accountId);
        return toMessageVO(message, account == null ? null : account.getUsername(), self.getRole());
    }

    @Override
    @Transactional
    public boolean leaveGroup(Integer accountId, Integer groupId) {
        StudyGroupMember self = membership(groupId, accountId);
        if (self == null) {
            return false;
        }
        if (StudyGroupMember.ROLE_OWNER.equals(self.getRole())) {
            // 组长退出即解散小组，成员与消息随外键级联删除
            removeById(groupId);
        } else {
            memberMapper.deleteById(self.getId());
        }
        return true;
    }

    @Override
    public boolean removeMember(Integer accountId, Integer groupId, Integer memberAccountId) {
        StudyGroupMember self = membership(groupId, accountId);
        if (self == null || !StudyGroupMember.ROLE_OWNER.equals(self.getRole())
                || accountId.equals(memberAccountId)) {
            return false;
        }
        StudyGroupMember target = membership(groupId, memberAccountId);
        if (target == null) {
            return false;
        }
        memberMapper.deleteById(target.getId());
        return true;
    }

    private StudyGroupMessage saveMessage(Integer groupId, Integer accountId, String type,
                                          String title, String content) {
        StudyGroupMessage message = new StudyGroupMessage();
        message.setGroupId(groupId);
        message.setAccountId(accountId);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    private StudyGroupMember membership(Integer groupId, Integer accountId) {
        return memberMapper.selectOne(
                new LambdaQueryWrapper<StudyGroupMember>()
                        .eq(StudyGroupMember::getGroupId, groupId)
                        .eq(StudyGroupMember::getAccountId, accountId));
    }

    private Map<Integer, String> memberRoles(Integer groupId) {
        return memberMapper.selectList(
                        new LambdaQueryWrapper<StudyGroupMember>()
                                .eq(StudyGroupMember::getGroupId, groupId))
                .stream()
                .collect(Collectors.toMap(StudyGroupMember::getAccountId, StudyGroupMember::getRole,
                        (a, b) -> a));
    }

    private Map<Integer, Long> memberCounts(Collection<Integer> groupIds) {
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

    private Map<Integer, String> usernames(Collection<Integer> accountIds) {
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

    private String normalizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("小组名称不能为空且不超过 " + MAX_NAME_LENGTH + " 字");
        }
        return trimmed;
    }

    private String normalizeContent(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("内容不能为空且不超过 " + MAX_CONTENT_LENGTH + " 字");
        }
        return trimmed;
    }

    /**
     * 生成不重复的邀请码，撞码时重试。
     */
    private String generateInviteCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                code.append(INVITE_CODE_ALPHABET.charAt(RANDOM.nextInt(INVITE_CODE_ALPHABET.length())));
            }
            if (query().eq("invite_code", code.toString()).count() == 0) {
                return code.toString();
            }
        }
        throw new IllegalStateException("邀请码生成失败，请重试");
    }

    private StudyGroupVO toVO(StudyGroup group, String role, String ownerName, int memberCount) {
        return new StudyGroupVO(
                group.getId(),
                group.getName(),
                group.getOwnerId(),
                ownerName,
                group.getInviteCode(),
                memberCount,
                role,
                group.getCreatedAt());
    }

    private GroupMessageVO toMessageVO(StudyGroupMessage message, String username, String role) {
        return new GroupMessageVO(
                message.getId(),
                message.getAccountId(),
                username,
                role,
                message.getType(),
                message.getTitle(),
                message.getContent(),
                message.getCreatedAt());
    }
}
