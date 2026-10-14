package com.tick.service;

import com.tick.entity.vo.request.GroupMessageCreateVO;
import com.tick.entity.vo.request.GroupTaskCreateVO;
import com.tick.entity.vo.request.StudyGroupCreateVO;
import com.tick.entity.vo.request.StudyGroupJoinVO;
import com.tick.entity.vo.response.GroupMessageVO;
import com.tick.entity.vo.response.StudyGroupDetailVO;
import com.tick.entity.vo.response.StudyGroupVO;

import java.util.List;

/**
 * 学习小组服务：创建/加入小组、邀请码入群、聊天消息与学习任务。
 * 一个人可以加入或创建多个小组；非成员不可查看小组内容与成员。
 */
public interface StudyGroupService {
    /**
     * 当前用户加入（含创建）的全部小组，按创建时间倒序。
     */
    List<StudyGroupVO> listMyGroups(Integer accountId);

    /**
     * 创建小组：生成唯一邀请码，创建者自动成为组长。
     */
    StudyGroupVO createGroup(Integer accountId, StudyGroupCreateVO vo);

    /**
     * 通过邀请码加入小组，已在组内或邀请码无效时抛 {@link IllegalArgumentException}。
     */
    StudyGroupVO joinByCode(Integer accountId, StudyGroupJoinVO vo);

    /**
     * 小组详情（含成员列表），仅成员可见，非成员返回 null。
     */
    StudyGroupDetailVO getDetail(Integer accountId, Integer groupId);

    /**
     * 聊天区消息（最近若干条，按时间正序），仅成员可见，非成员返回 null。
     */
    List<GroupMessageVO> listMessages(Integer accountId, Integer groupId);

    /**
     * 发布普通消息，组长组员均可，仅成员可发，非成员返回 null。
     */
    GroupMessageVO postMessage(Integer accountId, Integer groupId, GroupMessageCreateVO vo);

    /**
     * 发布学习任务，仅组长可发；非组长抛 {@link SecurityException}，非成员返回 null。
     */
    GroupMessageVO postTask(Integer accountId, Integer groupId, GroupTaskCreateVO vo);

    /**
     * 退出小组：普通成员直接退出；组长退出即解散小组（成员与消息级联删除）。
     * 非成员返回 false。
     */
    boolean leaveGroup(Integer accountId, Integer groupId);

    /**
     * 组长移除组员（不能移除自己）。当前用户不是组长返回 false。
     */
    boolean removeMember(Integer accountId, Integer groupId, Integer memberAccountId);
}
