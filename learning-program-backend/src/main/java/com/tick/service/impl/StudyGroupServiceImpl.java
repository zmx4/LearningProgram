package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Account;
import com.tick.entity.dto.Course;
import com.tick.entity.dto.CourseChapter;
import com.tick.entity.dto.CourseProgress;
import com.tick.entity.dto.GroupMemberVisit;
import com.tick.entity.dto.GroupTaskItem;
import com.tick.entity.dto.GroupTaskProgress;
import com.tick.entity.dto.LearningResource;
import com.tick.entity.dto.ResourceAccessLog;
import com.tick.entity.dto.StudyGroup;
import com.tick.entity.dto.StudyGroupMember;
import com.tick.entity.dto.StudyGroupMessage;
import com.tick.entity.vo.request.GroupMessageCreateVO;
import com.tick.entity.vo.request.GroupTaskCreateVO;
import com.tick.entity.vo.request.GroupTaskItemCreateVO;
import com.tick.entity.vo.request.StudyGroupCreateVO;
import com.tick.entity.vo.request.StudyGroupJoinVO;
import com.tick.entity.vo.response.GroupMemberDetailVO;
import com.tick.entity.vo.response.GroupMemberVO;
import com.tick.entity.vo.response.GroupMessageVO;
import com.tick.entity.vo.response.GroupTaskItemVO;
import com.tick.entity.vo.response.GroupTaskProgressVO;
import com.tick.entity.vo.response.GroupTaskVO;
import com.tick.entity.vo.response.MemberTaskProgressVO;
import com.tick.entity.vo.response.StudyGroupDetailVO;
import com.tick.entity.vo.response.StudyGroupVO;
import com.tick.exception.NotFoundException;
import com.tick.mapper.CourseChapterMapper;
import com.tick.mapper.CourseMapper;
import com.tick.mapper.CourseProgressMapper;
import com.tick.mapper.GroupMemberVisitMapper;
import com.tick.mapper.GroupTaskItemMapper;
import com.tick.mapper.GroupTaskProgressMapper;
import com.tick.mapper.LearningResourceMapper;
import com.tick.mapper.ResourceAccessLogMapper;
import com.tick.mapper.StudyGroupMapper;
import com.tick.mapper.StudyGroupMemberMapper;
import com.tick.mapper.StudyGroupMessageMapper;
import com.tick.service.AccountService;
import com.tick.service.StudyGroupService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * 学习任务本体是 type = task 的消息，绑定项（课程/资源/组员访问/纯文本）存 db_group_task_item；
 * 完成判定：课程按章节覆盖度、资源按任务发布后的访问流水、组员访问按任务发布后的小组访问记录
 * 实时计算，纯文本由组员手动标记并落 db_group_task_progress。
 */
@Service
public class StudyGroupServiceImpl extends ServiceImpl<StudyGroupMapper, StudyGroup>
        implements StudyGroupService {
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MESSAGE_LIMIT = 200;
    private static final int TASK_LIMIT = 100;
    private static final int MAX_TASK_ITEMS = 20;
    private static final int MAX_ITEM_TITLE_LENGTH = 200;
    private static final int INVITE_CODE_LENGTH = 8;
    private static final Set<String> ITEM_TYPES = Set.of(
            GroupTaskItem.TYPE_COURSE, GroupTaskItem.TYPE_RESOURCE,
            GroupTaskItem.TYPE_MEMBER_VISIT, GroupTaskItem.TYPE_TEXT);
    /** 去除易混淆字符的邀请码字母表 */
    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StudyGroupMemberMapper memberMapper;
    private final StudyGroupMessageMapper messageMapper;
    private final GroupTaskItemMapper taskItemMapper;
    private final GroupTaskProgressMapper taskProgressMapper;
    private final GroupMemberVisitMapper visitMapper;
    private final ResourceAccessLogMapper accessLogMapper;
    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseProgressMapper courseProgressMapper;
    private final LearningResourceMapper resourceMapper;
    private final AccountService accountService;

    public StudyGroupServiceImpl(StudyGroupMemberMapper memberMapper,
                                 StudyGroupMessageMapper messageMapper,
                                 GroupTaskItemMapper taskItemMapper,
                                 GroupTaskProgressMapper taskProgressMapper,
                                 GroupMemberVisitMapper visitMapper,
                                 ResourceAccessLogMapper accessLogMapper,
                                 CourseMapper courseMapper,
                                 CourseChapterMapper chapterMapper,
                                 CourseProgressMapper courseProgressMapper,
                                 LearningResourceMapper resourceMapper,
                                 AccountService accountService) {
        this.memberMapper = memberMapper;
        this.messageMapper = messageMapper;
        this.taskItemMapper = taskItemMapper;
        this.taskProgressMapper = taskProgressMapper;
        this.visitMapper = visitMapper;
        this.accessLogMapper = accessLogMapper;
        this.courseMapper = courseMapper;
        this.chapterMapper = chapterMapper;
        this.courseProgressMapper = courseProgressMapper;
        this.resourceMapper = resourceMapper;
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
        // 记录本次访问，供 member_visit 类型任务项判定"任务发布后组员已访问小组"
        recordVisit(groupId, accountId);
        List<StudyGroupMember> members = membersOf(groupId);
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
    @Transactional
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
        List<GroupTaskItem> items = normalizeItems(vo.getItems());
        StudyGroupMessage message = saveMessage(groupId, accountId, StudyGroupMessage.TYPE_TASK, title, content);
        for (int i = 0; i < items.size(); i++) {
            GroupTaskItem item = items.get(i);
            item.setGroupId(groupId);
            item.setMessageId(message.getId());
            item.setSortOrder(i);
            taskItemMapper.insert(item);
        }
        Account account = accountService.getById(accountId);
        return toMessageVO(message, account == null ? null : account.getUsername(), self.getRole());
    }

    @Override
    public List<GroupTaskVO> listTasks(Integer accountId, Integer groupId) {
        List<StudyGroupMember> members = membersOf(groupId);
        if (members.stream().noneMatch(member -> member.getAccountId().equals(accountId))) {
            return null;
        }
        List<StudyGroupMessage> tasks = taskMessages(groupId);
        if (tasks.isEmpty()) {
            return List.of();
        }
        CompletionContext ctx = completionContext(groupId, tasks, members);
        Map<Integer, String> names = usernames(tasks.stream().map(StudyGroupMessage::getAccountId).toList());
        return tasks.stream()
                .map(task -> toTaskVO(task, accountId, members, ctx, names.get(task.getAccountId())))
                .toList();
    }

    @Override
    @Transactional
    public GroupTaskVO completeTaskItem(Integer accountId, Integer groupId, Integer taskId, Integer itemId) {
        StudyGroupMember self = membership(groupId, accountId);
        if (self == null) {
            return null;
        }
        GroupTaskItem item = taskItemMapper.selectById(itemId);
        StudyGroupMessage task = item == null ? null : messageMapper.selectById(taskId);
        if (item == null || task == null
                || !groupId.equals(item.getGroupId()) || !taskId.equals(item.getMessageId())
                || !StudyGroupMessage.TYPE_TASK.equals(task.getType())) {
            return null;
        }
        if (!GroupTaskItem.TYPE_TEXT.equals(item.getItemType())) {
            throw new IllegalArgumentException("该任务项由系统自动判定完成，无需手动标记");
        }
        Long exists = taskProgressMapper.selectCount(new LambdaQueryWrapper<GroupTaskProgress>()
                .eq(GroupTaskProgress::getItemId, itemId)
                .eq(GroupTaskProgress::getAccountId, accountId));
        if (exists == null || exists == 0) {
            GroupTaskProgress progress = new GroupTaskProgress();
            progress.setItemId(itemId);
            progress.setAccountId(accountId);
            progress.setCompletedAt(LocalDateTime.now());
            try {
                taskProgressMapper.insert(progress);
            } catch (DuplicateKeyException ignored) {
                // 并发重复标记，唯一键兜底，幂等处理
            }
        }
        List<StudyGroupMember> members = membersOf(groupId);
        CompletionContext ctx = completionContext(groupId, List.of(task), members);
        Account publisher = accountService.getById(task.getAccountId());
        return toTaskVO(task, accountId, members, ctx,
                publisher == null ? null : publisher.getUsername());
    }

    @Override
    public GroupTaskProgressVO taskProgress(Integer accountId, Integer groupId, Integer taskId) {
        StudyGroupMember self = membership(groupId, accountId);
        if (self == null) {
            return null;
        }
        if (!StudyGroupMember.ROLE_OWNER.equals(self.getRole())) {
            throw new SecurityException("只有组长可以查看任务完成进度");
        }
        StudyGroupMessage task = messageMapper.selectById(taskId);
        if (task == null || !groupId.equals(task.getGroupId())
                || !StudyGroupMessage.TYPE_TASK.equals(task.getType())) {
            return null;
        }
        List<StudyGroupMember> members = membersOf(groupId);
        CompletionContext ctx = completionContext(groupId, List.of(task), members);
        List<GroupTaskItem> items = ctx.items(taskId);

        List<GroupTaskItemVO> itemVOs = new ArrayList<>();
        for (GroupTaskItem item : items) {
            int completedCount = 0;
            for (StudyGroupMember member : members) {
                if (itemState(ctx, item, member.getAccountId(), task.getCreatedAt()).completed()) {
                    completedCount++;
                }
            }
            itemVOs.add(new GroupTaskItemVO(item.getId(), item.getItemType(), item.getRefId(),
                    item.getTitle(), null, null, completedCount));
        }
        Map<Integer, String> names = usernames(members.stream().map(StudyGroupMember::getAccountId).toList());
        List<MemberTaskProgressVO> memberVOs = members.stream()
                .map(member -> memberProgress(ctx, task, items, member, names.get(member.getAccountId())))
                .toList();
        return new GroupTaskProgressVO(taskId, task.getTitle(), itemVOs, memberVOs);
    }

    @Override
    public GroupMemberDetailVO memberDetail(Integer accountId, Integer groupId, Integer memberAccountId) {
        if (membership(groupId, accountId) == null) {
            return null;
        }
        StudyGroupMember target = membership(groupId, memberAccountId);
        if (target == null) {
            return null;
        }
        Account account = accountService.getById(memberAccountId);
        List<StudyGroupMessage> tasks = taskMessages(groupId);
        List<StudyGroupMember> members = membersOf(groupId);
        CompletionContext ctx = tasks.isEmpty()
                ? CompletionContext.empty()
                : completionContext(groupId, tasks, members);
        List<GroupMemberDetailVO.TaskStatus> taskStatuses = tasks.stream()
                .map(task -> {
                    List<GroupTaskItem> items = ctx.items(task.getId());
                    List<GroupTaskItemVO> itemVOs = items.stream()
                            .map(item -> {
                                ItemState state = itemState(ctx, item, memberAccountId, task.getCreatedAt());
                                return new GroupTaskItemVO(item.getId(), item.getItemType(), item.getRefId(),
                                        item.getTitle(), state.completed(), state.completedAt(), null);
                            })
                            .toList();
                    long done = itemVOs.stream().filter(vo -> Boolean.TRUE.equals(vo.getMyCompleted())).count();
                    Integer percent = items.isEmpty() ? null
                            : (int) Math.round(done * 100.0 / items.size());
                    return new GroupMemberDetailVO.TaskStatus(task.getId(), task.getTitle(),
                            task.getCreatedAt(), (int) done, items.size(), percent, itemVOs);
                })
                .toList();
        return new GroupMemberDetailVO(memberAccountId,
                account == null ? null : account.getUsername(),
                account == null ? null : account.getEmail(),
                account == null ? null : account.getBio(),
                target.getRole(), target.getJoinedAt(), taskStatuses);
    }

    @Override
    @Transactional
    public boolean leaveGroup(Integer accountId, Integer groupId) {
        StudyGroupMember self = membership(groupId, accountId);
        if (self == null) {
            return false;
        }
        if (StudyGroupMember.ROLE_OWNER.equals(self.getRole())) {
            // 组长退出即解散小组，成员、消息、任务项与访问记录随外键级联删除
            removeById(groupId);
        } else {
            memberMapper.deleteById(self.getId());
        }
        return true;
    }

    @Override
    public void removeMember(Integer accountId, Integer groupId, Integer memberAccountId) {
        StudyGroupMember self = membership(groupId, accountId);
        if (self == null) {
            throw new NotFoundException("小组不存在或你不是小组成员");
        }
        if (!StudyGroupMember.ROLE_OWNER.equals(self.getRole())) {
            throw new SecurityException("只有组长可以移除组员");
        }
        if (accountId.equals(memberAccountId)) {
            throw new IllegalArgumentException("不能移除自己");
        }
        StudyGroupMember target = membership(groupId, memberAccountId);
        if (target == null) {
            throw new NotFoundException("该成员不在小组中");
        }
        memberMapper.deleteById(target.getId());
    }

    /**
     * 发布任务时校验并构建绑定项：course/resource 取关联实体标题，member_visit/text 用自定义标题。
     */
    private List<GroupTaskItem> normalizeItems(List<GroupTaskItemCreateVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return List.of();
        }
        if (vos.size() > MAX_TASK_ITEMS) {
            throw new IllegalArgumentException("任务绑定项最多 " + MAX_TASK_ITEMS + " 个");
        }
        List<GroupTaskItem> items = new ArrayList<>();
        for (GroupTaskItemCreateVO itemVO : vos) {
            String type = itemVO == null || itemVO.getType() == null ? "" : itemVO.getType().trim();
            if (!ITEM_TYPES.contains(type)) {
                throw new IllegalArgumentException("不支持的任务项类型");
            }
            GroupTaskItem item = new GroupTaskItem();
            item.setItemType(type);
            switch (type) {
                case GroupTaskItem.TYPE_COURSE -> {
                    Course course = itemVO.getRefId() == null ? null : courseMapper.selectById(itemVO.getRefId());
                    if (course == null) {
                        throw new IllegalArgumentException("绑定的课程不存在");
                    }
                    item.setRefId(course.getId());
                    item.setTitle(course.getTitle());
                }
                case GroupTaskItem.TYPE_RESOURCE -> {
                    LearningResource resource = itemVO.getRefId() == null ? null
                            : resourceMapper.selectById(itemVO.getRefId());
                    if (resource == null) {
                        throw new IllegalArgumentException("绑定的资源不存在");
                    }
                    item.setRefId(resource.getId());
                    item.setTitle(resource.getTitle());
                }
                default -> {
                    String title = itemVO.getTitle() == null ? "" : itemVO.getTitle().trim();
                    if (title.isEmpty() || title.length() > MAX_ITEM_TITLE_LENGTH) {
                        throw new IllegalArgumentException(
                                "任务项标题不能为空且不超过 " + MAX_ITEM_TITLE_LENGTH + " 字");
                    }
                    item.setTitle(title);
                }
            }
            items.add(item);
        }
        return items;
    }

    private MemberTaskProgressVO memberProgress(CompletionContext ctx, StudyGroupMessage task,
                                                List<GroupTaskItem> items, StudyGroupMember member,
                                                String username) {
        List<MemberTaskProgressVO.ItemStatus> statuses = items.stream()
                .map(item -> {
                    ItemState state = itemState(ctx, item, member.getAccountId(), task.getCreatedAt());
                    return new MemberTaskProgressVO.ItemStatus(item.getId(), state.completed(),
                            state.completedAt());
                })
                .toList();
        long done = statuses.stream().filter(MemberTaskProgressVO.ItemStatus::getCompleted).count();
        Integer percent = items.isEmpty() ? null : (int) Math.round(done * 100.0 / items.size());
        return new MemberTaskProgressVO(member.getAccountId(), username, member.getRole(),
                (int) done, items.size(), percent, statuses);
    }

    private GroupTaskVO toTaskVO(StudyGroupMessage task, Integer viewerId,
                                 List<StudyGroupMember> members, CompletionContext ctx, String username) {
        List<GroupTaskItem> items = ctx.items(task.getId());
        int memberCount = members.size();
        List<GroupTaskItemVO> itemVOs = new ArrayList<>();
        int myCompleted = 0;
        int totalCompletions = 0;
        for (GroupTaskItem item : items) {
            ItemState mine = itemState(ctx, item, viewerId, task.getCreatedAt());
            if (mine.completed()) {
                myCompleted++;
            }
            int completedCount = 0;
            for (StudyGroupMember member : members) {
                if (itemState(ctx, item, member.getAccountId(), task.getCreatedAt()).completed()) {
                    completedCount++;
                }
            }
            totalCompletions += completedCount;
            itemVOs.add(new GroupTaskItemVO(item.getId(), item.getItemType(), item.getRefId(),
                    item.getTitle(), mine.completed(), mine.completedAt(), completedCount));
        }
        Integer percent = items.isEmpty() || memberCount == 0 ? null
                : (int) Math.round(totalCompletions * 100.0 / (items.size() * (double) memberCount));
        return new GroupTaskVO(task.getId(), task.getTitle(), task.getContent(), task.getAccountId(),
                username, task.getCreatedAt(), itemVOs, memberCount, myCompleted, items.size(),
                totalCompletions, percent);
    }

    /**
     * 单个任务项对某个成员的完成判定：
     * text 看手动标记；course 看章节是否全部学完；resource / member_visit
     * 看任务发布之后是否有对应的访问记录。
     */
    private ItemState itemState(CompletionContext ctx, GroupTaskItem item,
                                Integer accountId, LocalDateTime taskCreatedAt) {
        switch (item.getItemType()) {
            case GroupTaskItem.TYPE_TEXT -> {
                LocalDateTime at = ctx.manualCompletedAt(item.getId(), accountId);
                return new ItemState(at != null, at);
            }
            case GroupTaskItem.TYPE_COURSE -> {
                long total = ctx.chapterCount(item.getRefId());
                CourseStudy study = ctx.courseStudy(accountId, item.getRefId());
                boolean completed = total > 0 && study != null && study.count >= total;
                return new ItemState(completed, completed ? study.lastAt : null);
            }
            case GroupTaskItem.TYPE_RESOURCE -> {
                LocalDateTime at = ctx.resourceAccessAt(accountId, item.getRefId());
                boolean completed = at != null && (taskCreatedAt == null || !at.isBefore(taskCreatedAt));
                return new ItemState(completed, completed ? at : null);
            }
            case GroupTaskItem.TYPE_MEMBER_VISIT -> {
                LocalDateTime at = ctx.visitAt(accountId);
                boolean completed = at != null && (taskCreatedAt == null || !at.isBefore(taskCreatedAt));
                return new ItemState(completed, completed ? at : null);
            }
            default -> {
                return new ItemState(false, null);
            }
        }
    }

    /**
     * 批量加载一组任务的完成判定上下文，避免按项逐次查库。
     * 只统计本组成员、且只读取任务发布之后的资源访问流水，避免随流水表增长而全量扫描。
     */
    private CompletionContext completionContext(Integer groupId, List<StudyGroupMessage> tasks,
                                                List<StudyGroupMember> members) {
        List<Integer> messageIds = tasks.stream().map(StudyGroupMessage::getId).toList();
        List<GroupTaskItem> items = taskItemMapper.selectList(
                new LambdaQueryWrapper<GroupTaskItem>()
                        .in(GroupTaskItem::getMessageId, messageIds)
                        .orderByAsc(GroupTaskItem::getSortOrder)
                        .orderByAsc(GroupTaskItem::getId));
        Map<Integer, List<GroupTaskItem>> itemsByMessage = items.stream()
                .collect(Collectors.groupingBy(GroupTaskItem::getMessageId));

        Map<String, LocalDateTime> manual = new HashMap<>();
        List<Integer> itemIds = items.stream().map(GroupTaskItem::getId).toList();
        if (!itemIds.isEmpty()) {
            for (GroupTaskProgress row : taskProgressMapper.selectList(
                    new LambdaQueryWrapper<GroupTaskProgress>()
                            .in(GroupTaskProgress::getItemId, itemIds))) {
                manual.put(row.getItemId() + "_" + row.getAccountId(), row.getCompletedAt());
            }
        }

        List<Integer> memberIds = members.stream().map(StudyGroupMember::getAccountId).toList();
        LocalDateTime taskWindowStart = tasks.stream()
                .map(StudyGroupMessage::getCreatedAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        Set<Integer> courseIds = items.stream()
                .filter(item -> GroupTaskItem.TYPE_COURSE.equals(item.getItemType()))
                .map(GroupTaskItem::getRefId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Long> chapterCounts = new HashMap<>();
        Map<String, CourseStudy> studies = new HashMap<>();
        if (!courseIds.isEmpty()) {
            for (CourseChapter chapter : chapterMapper.selectList(
                    new LambdaQueryWrapper<CourseChapter>()
                            .select(CourseChapter::getCourseId)
                            .in(CourseChapter::getCourseId, courseIds))) {
                chapterCounts.merge(chapter.getCourseId(), 1L, Long::sum);
            }
            if (!memberIds.isEmpty()) {
                for (CourseProgress row : courseProgressMapper.selectList(
                        new LambdaQueryWrapper<CourseProgress>()
                                .select(CourseProgress::getAccountId, CourseProgress::getCourseId,
                                        CourseProgress::getUpdatedAt)
                                .in(CourseProgress::getCourseId, courseIds)
                                .in(CourseProgress::getAccountId, memberIds))) {
                    studies.compute(row.getAccountId() + "_" + row.getCourseId(), (key, study) -> {
                        if (study == null) {
                            study = new CourseStudy();
                        }
                        study.count++;
                        if (study.lastAt == null || (row.getUpdatedAt() != null
                                && row.getUpdatedAt().isAfter(study.lastAt))) {
                            study.lastAt = row.getUpdatedAt();
                        }
                        return study;
                    });
                }
            }
        }

        Set<Integer> resourceIds = items.stream()
                .filter(item -> GroupTaskItem.TYPE_RESOURCE.equals(item.getItemType()))
                .map(GroupTaskItem::getRefId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, LocalDateTime> accesses = new HashMap<>();
        if (!resourceIds.isEmpty() && !memberIds.isEmpty()) {
            for (ResourceAccessLog log : accessLogMapper.selectList(
                    new LambdaQueryWrapper<ResourceAccessLog>()
                            .select(ResourceAccessLog::getAccountId, ResourceAccessLog::getResourceId,
                                    ResourceAccessLog::getAccessedAt)
                            .in(ResourceAccessLog::getResourceId, resourceIds)
                            .in(ResourceAccessLog::getAccountId, memberIds)
                            .ge(taskWindowStart != null, ResourceAccessLog::getAccessedAt, taskWindowStart))) {
                accesses.merge(log.getAccountId() + "_" + log.getResourceId(), log.getAccessedAt(),
                        (a, b) -> a.isAfter(b) ? a : b);
            }
        }

        Map<Integer, LocalDateTime> visits = new HashMap<>();
        for (GroupMemberVisit visit : visitMapper.selectList(
                new LambdaQueryWrapper<GroupMemberVisit>()
                        .eq(GroupMemberVisit::getGroupId, groupId))) {
            visits.put(visit.getAccountId(), visit.getLastVisitAt());
        }
        return new CompletionContext(itemsByMessage, manual, chapterCounts, studies, accesses, visits);
    }

    private List<StudyGroupMessage> taskMessages(Integer groupId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<StudyGroupMessage>()
                        .eq(StudyGroupMessage::getGroupId, groupId)
                        .eq(StudyGroupMessage::getType, StudyGroupMessage.TYPE_TASK)
                        .orderByAsc(StudyGroupMessage::getId)
                        .last("LIMIT " + TASK_LIMIT));
    }

    private List<StudyGroupMember> membersOf(Integer groupId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<StudyGroupMember>()
                        .eq(StudyGroupMember::getGroupId, groupId)
                        .orderByAsc(StudyGroupMember::getJoinedAt));
    }

    /**
     * 记录成员访问小组详情页的时间，已存在则刷新。
     */
    private void recordVisit(Integer groupId, Integer accountId) {
        GroupMemberVisit existing = visitMapper.selectOne(
                new LambdaQueryWrapper<GroupMemberVisit>()
                        .eq(GroupMemberVisit::getGroupId, groupId)
                        .eq(GroupMemberVisit::getAccountId, accountId));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            GroupMemberVisit visit = new GroupMemberVisit();
            visit.setGroupId(groupId);
            visit.setAccountId(accountId);
            visit.setLastVisitAt(now);
            try {
                visitMapper.insert(visit);
            } catch (DuplicateKeyException ignored) {
                // 并发首次访问，唯一键兜底，忽略即可
            }
        } else {
            existing.setLastVisitAt(now);
            visitMapper.updateById(existing);
        }
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
                code.append(INVITE_CODE_ALPHABET.charAt(RANDOM.nextInt(INVITE_CODE_LENGTH)));
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

    /** 任务项对单个成员的完成状态。 */
    private record ItemState(boolean completed, LocalDateTime completedAt) {
    }

    /** 某成员在某课程下已学章节数与最近学习时间。 */
    private static final class CourseStudy {
        private long count;
        private LocalDateTime lastAt;
    }

    /** 一组任务的完成判定上下文（批量预加载）。 */
    private record CompletionContext(Map<Integer, List<GroupTaskItem>> itemsByMessage,
                                     Map<String, LocalDateTime> manual,
                                     Map<Integer, Long> chapterCounts,
                                     Map<String, CourseStudy> studies,
                                     Map<String, LocalDateTime> accesses,
                                     Map<Integer, LocalDateTime> visits) {
        private static CompletionContext empty() {
            return new CompletionContext(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }

        private List<GroupTaskItem> items(Integer messageId) {
            return itemsByMessage.getOrDefault(messageId, List.of());
        }

        private LocalDateTime manualCompletedAt(Integer itemId, Integer accountId) {
            return manual.get(itemId + "_" + accountId);
        }

        private long chapterCount(Integer courseId) {
            return chapterCounts.getOrDefault(courseId, 0L);
        }

        private CourseStudy courseStudy(Integer accountId, Integer courseId) {
            return studies.get(accountId + "_" + courseId);
        }

        private LocalDateTime resourceAccessAt(Integer accountId, Integer resourceId) {
            return accesses.get(accountId + "_" + resourceId);
        }

        private LocalDateTime visitAt(Integer accountId) {
            return visits.get(accountId);
        }
    }
}
