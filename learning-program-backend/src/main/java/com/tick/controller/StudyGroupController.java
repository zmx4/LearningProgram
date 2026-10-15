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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public RestBean<List<StudyGroupVO>> list(HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        return RestBean.success(studyGroupService.listMyGroups(accountId));
    }

    @PostMapping
    public RestBean<StudyGroupVO> create(@RequestBody StudyGroupCreateVO vo, HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        try {
            return RestBean.success(studyGroupService.createGroup(accountId, vo));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @PostMapping("/join")
    public RestBean<StudyGroupVO> join(@RequestBody StudyGroupJoinVO vo, HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        try {
            return RestBean.success(studyGroupService.joinByCode(accountId, vo));
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @GetMapping("/{id}")
    public RestBean<StudyGroupDetailVO> detail(@PathVariable Integer id, HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        StudyGroupDetailVO detail = studyGroupService.getDetail(accountId, id);
        return detail == null
                ? RestBean.failure(404, "小组不存在或你不是小组成员")
                : RestBean.success(detail);
    }

    @GetMapping("/{id}/messages")
    public RestBean<List<GroupMessageVO>> messages(@PathVariable Integer id, HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        List<GroupMessageVO> messages = studyGroupService.listMessages(accountId, id);
        return messages == null
                ? RestBean.failure(404, "小组不存在或你不是小组成员")
                : RestBean.success(messages);
    }

    @PostMapping("/{id}/messages")
    public RestBean<GroupMessageVO> postMessage(@PathVariable Integer id,
                                                @RequestBody GroupMessageCreateVO vo,
                                                HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        try {
            GroupMessageVO message = studyGroupService.postMessage(accountId, id, vo);
            return message == null
                    ? RestBean.failure(404, "小组不存在或你不是小组成员")
                    : RestBean.success(message);
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @PostMapping("/{id}/tasks")
    public RestBean<GroupMessageVO> postTask(@PathVariable Integer id,
                                             @RequestBody GroupTaskCreateVO vo,
                                             HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        try {
            GroupMessageVO message = studyGroupService.postTask(accountId, id, vo);
            return message == null
                    ? RestBean.failure(404, "小组不存在或你不是小组成员")
                    : RestBean.success(message);
        } catch (SecurityException exception) {
            return RestBean.forbidden(exception.getMessage());
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @GetMapping("/{id}/tasks")
    public RestBean<List<GroupTaskVO>> tasks(@PathVariable Integer id, HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        List<GroupTaskVO> tasks = studyGroupService.listTasks(accountId, id);
        return tasks == null
                ? RestBean.failure(404, "小组不存在或你不是小组成员")
                : RestBean.success(tasks);
    }

    @GetMapping("/{id}/tasks/{taskId}/progress")
    public RestBean<GroupTaskProgressVO> taskProgress(@PathVariable Integer id,
                                                      @PathVariable Integer taskId,
                                                      HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        try {
            GroupTaskProgressVO progress = studyGroupService.taskProgress(accountId, id, taskId);
            return progress == null
                    ? RestBean.failure(404, "任务不存在或你不是小组成员")
                    : RestBean.success(progress);
        } catch (SecurityException exception) {
            return RestBean.forbidden(exception.getMessage());
        }
    }

    @PostMapping("/{id}/tasks/{taskId}/items/{itemId}/complete")
    public RestBean<GroupTaskVO> completeTaskItem(@PathVariable Integer id,
                                                  @PathVariable Integer taskId,
                                                  @PathVariable Integer itemId,
                                                  HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        try {
            GroupTaskVO task = studyGroupService.completeTaskItem(accountId, id, taskId, itemId);
            return task == null
                    ? RestBean.failure(404, "任务项不存在或你不是小组成员")
                    : RestBean.success(task);
        } catch (IllegalArgumentException exception) {
            return RestBean.failure(400, exception.getMessage());
        }
    }

    @GetMapping("/{id}/members/{memberId}")
    public RestBean<GroupMemberDetailVO> memberDetail(@PathVariable Integer id,
                                                      @PathVariable Integer memberId,
                                                      HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        GroupMemberDetailVO detail = studyGroupService.memberDetail(accountId, id, memberId);
        return detail == null
                ? RestBean.failure(404, "成员不存在或你不是小组成员")
                : RestBean.success(detail);
    }

    @PostMapping("/{id}/leave")
    public RestBean<Void> leave(@PathVariable Integer id, HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        return studyGroupService.leaveGroup(accountId, id)
                ? RestBean.success()
                : RestBean.failure(404, "小组不存在或你不是小组成员");
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public RestBean<Void> removeMember(@PathVariable Integer id,
                                       @PathVariable Integer memberId,
                                       HttpServletRequest request) {
        Integer accountId = accountId(request);
        if (accountId == null) {
            return RestBean.unauthorized("登录状态无效");
        }
        return studyGroupService.removeMember(accountId, id, memberId)
                ? RestBean.success()
                : RestBean.forbidden("只有组长可以移除组员");
    }

    private Integer accountId(HttpServletRequest request) {
        return (Integer) request.getAttribute("id");
    }
}
