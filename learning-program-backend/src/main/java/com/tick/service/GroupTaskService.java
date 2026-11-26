package com.tick.service;

import com.tick.entity.vo.request.GroupTaskCreateVO;
import com.tick.entity.vo.response.GroupMemberDetailVO;
import com.tick.entity.vo.response.GroupMessageVO;
import com.tick.entity.vo.response.GroupTaskProgressVO;
import com.tick.entity.vo.response.GroupTaskVO;

import java.util.List;

/**
 * 小组学习任务服务：任务发布、任务查询、完成判定与成员任务进度。
 * 任务本体是 type = task 的小组消息，任务项用于追踪组员完成情况。
 */
public interface GroupTaskService {
    /**
     * 发布学习任务，仅组长可发；非组长抛 {@link SecurityException}，非成员返回 null。
     * 可同时绑定任务项（课程/资源/组员访问/纯文本），用于追踪组员完成情况。
     */
    GroupMessageVO postTask(Integer accountId, Integer groupId, GroupTaskCreateVO vo);

    /**
     * 小组全部学习任务（含绑定项、当前用户完成状态与全组总进度），仅成员可见，非成员返回 null。
     */
    List<GroupTaskVO> listTasks(Integer accountId, Integer groupId);

    /**
     * 组员手动标记纯文本任务项完成（幂等）。自动判定类任务项抛 {@link IllegalArgumentException}；
     * 非成员或任务项不属于该小组返回 null。完成后返回该任务的最新进度。
     */
    GroupTaskVO completeTaskItem(Integer accountId, Integer groupId, Integer taskId, Integer itemId);

    /**
     * 组长查看单个任务的完成矩阵（任务项 × 成员）。非组长抛 {@link SecurityException}，
     * 非成员或任务不存在返回 null。
     */
    GroupTaskProgressVO taskProgress(Integer accountId, Integer groupId, Integer taskId);

    /**
     * 小组成员详细信息：基本信息 + 该成员在各任务上的完成进度。仅成员可见，
     * 非成员或目标不在小组中返回 null。
     */
    GroupMemberDetailVO memberDetail(Integer accountId, Integer groupId, Integer memberAccountId);
}