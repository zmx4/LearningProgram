package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 组长视角下，单个成员在某个学习任务上的完成进度。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberTaskProgressVO {
    private Integer accountId;
    private String username;
    private String role;
    private Integer completedItems;
    private Integer totalItems;
    /** 完成百分比 0-100 */
    private Integer percent;
    private List<ItemStatus> itemStatuses;

    /**
     * 成员对单个任务项的完成状态。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemStatus {
        private Integer itemId;
        private Boolean completed;
        private LocalDateTime completedAt;
    }
}
