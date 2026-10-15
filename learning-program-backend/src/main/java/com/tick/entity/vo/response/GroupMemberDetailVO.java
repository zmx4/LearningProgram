package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小组成员详细信息：基本信息 + 该成员在各学习任务上的完成进度。仅小组成员可见。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDetailVO {
    private Integer accountId;
    private String username;
    private String email;
    private String bio;
    private String role;
    private LocalDateTime joinedAt;
    private List<TaskStatus> tasks;

    /**
     * 成员在单个学习任务上的完成情况汇总；items 中 myCompleted 表示该成员是否完成该项。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskStatus {
        private Integer taskId;
        private String title;
        private LocalDateTime createdAt;
        private Integer completedItems;
        private Integer totalItems;
        /** 完成百分比 0-100，无绑定项时为 null */
        private Integer percent;
        private List<GroupTaskItemVO> items;
    }
}
