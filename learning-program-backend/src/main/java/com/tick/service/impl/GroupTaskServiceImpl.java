package com.tick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tick.entity.dto.Account;
import com.tick.entity.dto.Course;
import com.tick.entity.dto.GroupTaskItem;
import com.tick.entity.dto.GroupTaskProgress;
import com.tick.entity.dto.LearningResource;
import com.tick.entity.dto.StudyGroupMember;
import com.tick.entity.dto.StudyGroupMessage;
import com.tick.entity.vo.request.GroupTaskCreateVO;
import com.tick.entity.vo.request.GroupTaskItemCreateVO;
import com.tick.entity.vo.response.GroupMemberDetailVO;
import com.tick.entity.vo.response.GroupMessageVO;
import com.tick.entity.vo.response.GroupTaskItemVO;
import com.tick.entity.vo.response.GroupTaskProgressVO;
import com.tick.entity.vo.response.GroupTaskVO;
import com.tick.entity.vo.response.MemberTaskProgressVO;
import com.tick.mapper.CourseMapper;
import com.tick.mapper.GroupTaskItemMapper;
import com.tick.mapper.GroupTaskProgressMapper;
import com.tick.mapper.LearningResourceMapper;
import com.tick.mapper.StudyGroupMessageMapper;
import com.tick.monitor.Monitored;
import com.tick.service.AccountService;
import com.tick.service.GroupTaskService;
import com.tick.service.impl.group.GroupTaskCompletionContext;
import com.tick.service.impl.group.GroupTaskItemState;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 小组学习任务服务实现。学习任务本体是 type = task 的消息，绑定项（课程/资源/组员访问/纯文本）
 * 存 db_group_task_item；完成判定：课程按章节覆盖度、资源按任务发布后的访问流水、
 * 组员访问按任务发布后的小组访问记录实时计算，纯文本由组员手动标记并落 db_group_task_progress。
 */
@Service
public class GroupTaskServiceImpl implements GroupTaskService {
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int TASK_LIMIT = 100;
    private static final int MAX_TASK_ITEMS = 20;
    private static final int MAX_ITEM_TITLE_LENGTH = 200;
    private static final Set<String> ITEM_TYPES = Set.of(
            GroupTaskItem.TYPE_COURSE, GroupTaskItem.TYPE_RESOURCE,
            GroupTaskItem.TYPE_MEMBER_VISIT, GroupTaskItem.TYPE_TEXT);

    private final StudyGroupMemberQuery memberQuery;
    private final StudyGroupMessageMapper messageMapper;
    private final GroupTaskItemMapper taskItemMapper;
    private final GroupTaskProgressMapper taskProgressMapper;
    private final CourseMapper courseMapper;
    private final LearningResourceMapper resourceMapper;
    private final AccountService accountService;
    private final GroupTaskCompletionService completionService;

    public GroupTaskServiceImpl(StudyGroupMemberQuery memberQuery,
                                StudyGroupMessageMapper messageMapper,
                                GroupTaskItemMapper taskItemMapper,
                                GroupTaskProgressMapper taskProgressMapper,
                                CourseMapper courseMapper,
                                LearningResourceMapper resourceMapper,
                                AccountService accountService,
                                GroupTaskCompletionService completionService) {
        this.memberQuery = memberQuery;
        this.messageMapper = messageMapper;
        this.taskItemMapper = taskItemMapper;
        this.taskProgressMapper = taskProgressMapper;
        this.courseMapper = courseMapper;
        this.resourceMapper = resourceMapper;
        this.accountService = accountService;
        this.completionService = completionService;
    }

    @Override
    @Transactional
    public GroupMessageVO postTask(Integer accountId, Integer groupId, GroupTaskCreateVO vo) {
        StudyGroupMember self = memberQuery.membership(groupId, accountId);
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
        List<StudyGroupMember> members = memberQuery.membersOf(groupId);
        if (members.stream().noneMatch(member -> member.getAccountId().equals(accountId))) {
            return null;
        }
        List<StudyGroupMessage> tasks = taskMessages(groupId);
        if (tasks.isEmpty()) {
            return List.of();
        }
        GroupTaskCompletionContext ctx = completionService.loadContext(groupId, tasks, members);
        Map<Integer, String> names = memberQuery.usernames(tasks.stream().map(StudyGroupMessage::getAccountId).toList());
        return tasks.stream()
                .map(task -> toTaskVO(task, accountId, members, ctx, names.get(task.getAccountId())))
                .toList();
    }

    @Override
    @Transactional
    @Monitored(slowThresholdMillis = 800)
    public GroupTaskVO completeTaskItem(Integer accountId, Integer groupId, Integer taskId, Integer itemId) {
        StudyGroupMember self = memberQuery.membership(groupId, accountId);
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
        List<StudyGroupMember> members = memberQuery.membersOf(groupId);
        GroupTaskCompletionContext ctx = completionService.loadContext(groupId, List.of(task), members);
        Account publisher = accountService.getById(task.getAccountId());
        return toTaskVO(task, accountId, members, ctx,
                publisher == null ? null : publisher.getUsername());
    }

    @Override
    public GroupTaskProgressVO taskProgress(Integer accountId, Integer groupId, Integer taskId) {
        StudyGroupMember self = memberQuery.membership(groupId, accountId);
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
        List<StudyGroupMember> members = memberQuery.membersOf(groupId);
        GroupTaskCompletionContext ctx = completionService.loadContext(groupId, List.of(task), members);
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
        Map<Integer, String> names = memberQuery.usernames(members.stream().map(StudyGroupMember::getAccountId).toList());
        List<MemberTaskProgressVO> memberVOs = members.stream()
                .map(member -> memberProgress(ctx, task, items, member, names.get(member.getAccountId())))
                .toList();
        return new GroupTaskProgressVO(taskId, task.getTitle(), itemVOs, memberVOs);
    }

    @Override
    public GroupMemberDetailVO memberDetail(Integer accountId, Integer groupId, Integer memberAccountId) {
        if (memberQuery.membership(groupId, accountId) == null) {
            return null;
        }
        StudyGroupMember target = memberQuery.membership(groupId, memberAccountId);
        if (target == null) {
            return null;
        }
        Account account = accountService.getById(memberAccountId);
        List<StudyGroupMessage> tasks = taskMessages(groupId);
        List<StudyGroupMember> members = memberQuery.membersOf(groupId);
        GroupTaskCompletionContext ctx = tasks.isEmpty()
                ? GroupTaskCompletionContext.empty()
                : completionService.loadContext(groupId, tasks, members);
        List<GroupMemberDetailVO.TaskStatus> taskStatuses = tasks.stream()
                .map(task -> {
                    List<GroupTaskItem> items = ctx.items(task.getId());
                    List<GroupTaskItemVO> itemVOs = items.stream()
                            .map(item -> {
                                GroupTaskItemState state = itemState(ctx, item, memberAccountId, task.getCreatedAt());
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

    private MemberTaskProgressVO memberProgress(GroupTaskCompletionContext ctx, StudyGroupMessage task,
                                                List<GroupTaskItem> items, StudyGroupMember member,
                                                String username) {
        List<MemberTaskProgressVO.ItemStatus> statuses = items.stream()
                .map(item -> {
                    GroupTaskItemState state = itemState(ctx, item, member.getAccountId(), task.getCreatedAt());
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
                                 List<StudyGroupMember> members, GroupTaskCompletionContext ctx, String username) {
        List<GroupTaskItem> items = ctx.items(task.getId());
        int memberCount = members.size();
        List<GroupTaskItemVO> itemVOs = new ArrayList<>();
        int myCompleted = 0;
        int totalCompletions = 0;
        for (GroupTaskItem item : items) {
            GroupTaskItemState mine = itemState(ctx, item, viewerId, task.getCreatedAt());
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

    private GroupTaskItemState itemState(GroupTaskCompletionContext ctx, GroupTaskItem item,
                                         Integer accountId, LocalDateTime taskCreatedAt) {
        return completionService.state(ctx, item, accountId, taskCreatedAt);
    }

    private List<StudyGroupMessage> taskMessages(Integer groupId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<StudyGroupMessage>()
                        .eq(StudyGroupMessage::getGroupId, groupId)
                        .eq(StudyGroupMessage::getType, StudyGroupMessage.TYPE_TASK)
                        .orderByAsc(StudyGroupMessage::getId)
                        .last("LIMIT " + TASK_LIMIT));
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

    private String normalizeContent(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("内容不能为空且不超过 " + MAX_CONTENT_LENGTH + " 字");
        }
        return trimmed;
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