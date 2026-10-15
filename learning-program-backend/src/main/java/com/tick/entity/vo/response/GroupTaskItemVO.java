package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 小组任务的单个绑定项及其完成情况。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupTaskItemVO {
    private Integer id;
    /** course / resource / member_visit / text */
    private String type;
    private Integer refId;
    private String title;
    /** 当前用户是否已完成该项 */
    private Boolean myCompleted;
    /** 当前用户完成时间（手动标记类有值，自动判定类取判定依据时间，未完成时为 null） */
    private LocalDateTime myCompletedAt;
    /** 全组成员中已完成该项的人数 */
    private Integer completedCount;
}
