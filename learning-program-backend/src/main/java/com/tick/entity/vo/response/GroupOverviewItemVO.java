package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 首页小组卡片及其当前用户任务汇总。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupOverviewItemVO {
    private Integer id;
    private String name;
    private Integer ownerId;
    private String ownerName;
    private String inviteCode;
    private Integer memberCount;
    private String role;
    private LocalDateTime createdAt;
    private Integer ongoingTasks;
    private Integer overdueTasks;
    private Integer completedTasks;
}
