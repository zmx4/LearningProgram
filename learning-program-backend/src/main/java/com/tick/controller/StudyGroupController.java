package com.tick.controller;

import com.tick.entity.RestBean;
import com.tick.entity.vo.request.GroupMessageCreateVO;
import com.tick.entity.vo.request.GroupTaskCreateVO;
import com.tick.entity.vo.request.StudyGroupCreateVO;
import com.tick.entity.vo.request.StudyGroupJoinVO;
import com.tick.entity.vo.response.GroupMemberDetailVO;
import com.tick.entity.vo.response.GroupMessageVO;
import com.tick.entity.vo.response.GroupTaskProgressVO;
import com.tick.entity.vo.response.GroupTaskVO;
import com.tick.entity.vo.response.StudyGroupDetailVO;
import com.tick.entity.vo.response.StudyGroupVO;
import com.tick.service.StudyGroupService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学习小组接口：创建/加入小组、邀请码入群、聊天消息与学习任务。
 * 小组内容仅成员可见；发布学习任务与移除组员仅限组长。
 */
@RestController
@RequestMapping("/api/groups")
public class StudyGroupController {
    private final StudyGroupService studyGroupService;

    public StudyGroupController(StudyGroupService studyGroupService) {
        this.studyGroupService = studyGroupService;
    }

    @GetMapping
    public RestBean<List<StudyGroupVO>> list(@RequestAttribute("id") Integer accountId) {
        return RestBean.success(studyGroupService.listMyGroups(accountId));
    }

    @PostMapping
    public RestBean<StudyGroupVO> create(@RequestBody StudyGroupCreateVO vo,
                                         @RequestAttribute("id") Integer accountId) {
        try {
            return RestBean.success(studyGroupService.createGroup(accountId, vo));
        } catch (IllegalStateException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @PostMapping("/join")
    public RestBean<StudyGroupVO> join(@RequestBody StudyGroupJoinVO vo,
                                       @RequestAttribute("id") Integer accountId) {
        return RestBean.success(studyGroupService.joinByCode(accountId, vo));
    }

    @GetMapping("/{id}")
    public RestBean<StudyGroupDetailVO> detail(@PathVariable Integer id,
                                               @RequestAttribute("id") Integer accountId) {
        StudyGroupDetailVO detail = studyGroupService.getDetail(accountId, id);
        return detail == null
                ? RestBean.failure(404, "小组不存在或你不是小组成员")
                : RestBean.success(detail);
    }

    @GetMapping("/{id}/messages")
    public RestBean<List<GroupMessageVO>> messages(@PathVariable Integer id,
                                                   @RequestAttribute("id") Integer accountId) {
        List<GroupMessageVO> messages = studyGroupService.listMessages(accountId, id);
        return messages == null
                ? RestBean.failure(404, "小组不存在或你不是小组成员")
                : RestBean.success(messages);
    }

    @PostMapping("/{id}/messages")
    public RestBean<GroupMessageVO> postMessage(@PathVariable Integer id,
                                                @RequestBody GroupMessageCreateVO vo,
                                                @RequestAttribute("id") Integer accountId) {
        GroupMessageVO message = studyGroupService.postMessage(accountId, id, vo);
        return message == null
                ? RestBean.failure(404, "小组不存在或你不是小组成员")
                : RestBean.success(message);
    }

    @PostMapping("/{id}/tasks")
    public RestBean<GroupMessageVO> postTask(@PathVariable Integer id,
                                             @RequestBody GroupTaskCreateVO vo,
                                             @RequestAttribute("id") Integer accountId) {
        GroupMessageVO message = studyGroupService.postTask(accountId, id, vo);
        return message == null
                ? RestBean.failure(404, "小组不存在或你不是小组成员")
                : RestBean.success(message);
    }

    @GetMapping("/{id}/tasks")
    public RestBean<List<GroupTaskVO>> tasks(@PathVariable Integer id,
                                             @RequestAttribute("id") Integer accountId) {
        List<GroupTaskVO> tasks = studyGroupService.listTasks(accountId, id);
        return tasks == null
                ? RestBean.failure(404, "小组不存在或你不是小组成员")
                : RestBean.success(tasks);
    }

    @GetMapping("/{id}/tasks/{taskId}/progress")
    public RestBean<GroupTaskProgressVO> taskProgress(@PathVariable Integer id,
                                                      @PathVariable Integer taskId,
                                                      @RequestAttribute("id") Integer accountId) {
        GroupTaskProgressVO progress = studyGroupService.taskProgress(accountId, id, taskId);
        return progress == null
                ? RestBean.failure(404, "任务不存在或你不是小组成员")
                : RestBean.success(progress);
    }

    @PostMapping("/{id}/tasks/{taskId}/items/{itemId}/complete")
    public RestBean<GroupTaskVO> completeTaskItem(@PathVariable Integer id,
                                                  @PathVariable Integer taskId,
                                                  @PathVariable Integer itemId,
                                                  @RequestAttribute("id") Integer accountId) {
        GroupTaskVO task = studyGroupService.completeTaskItem(accountId, id, taskId, itemId);
        return task == null
                ? RestBean.failure(404, "任务项不存在或你不是小组成员")
                : RestBean.success(task);
    }

    @GetMapping("/{id}/members/{memberId}")
    public RestBean<GroupMemberDetailVO> memberDetail(@PathVariable Integer id,
                                                      @PathVariable Integer memberId,
                                                      @RequestAttribute("id") Integer accountId) {
        GroupMemberDetailVO detail = studyGroupService.memberDetail(accountId, id, memberId);
        return detail == null
                ? RestBean.failure(404, "成员不存在或你不是小组成员")
                : RestBean.success(detail);
    }

    @PostMapping("/{id}/leave")
    public RestBean<Void> leave(@PathVariable Integer id,
                                @RequestAttribute("id") Integer accountId) {
        return studyGroupService.leaveGroup(accountId, id)
                ? RestBean.success()
                : RestBean.failure(404, "小组不存在或你不是小组成员");
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public RestBean<Void> removeMember(@PathVariable Integer id,
                                       @PathVariable Integer memberId,
                                       @RequestAttribute("id") Integer accountId) {
        return studyGroupService.removeMember(accountId, id, memberId)
                ? RestBean.success()
                : RestBean.forbidden("只有组长可以移除组员");
    }
}
