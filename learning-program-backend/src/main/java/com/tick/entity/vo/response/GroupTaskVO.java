package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 小组学习任务及其完成进度总览。id 即任务消息 id（db_study_group_message.id）。
 * progressPercent = 全部成员完成项次合计 / (任务项数 × 成员数)，无绑定项时为 null。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupTaskVO {
    private Integer id;
    private String title;
    private String content;
    private Integer accountId;
    private String username;
    private LocalDateTime createdAt;
    private List<GroupTaskItemVO> items;
    /** 小组当前成员数（含组长） */
    private Integer memberCount;
    /** 当前用户已完成的任务项数 */
    private Integer myCompleted;
    /** 任务项总数 */
    private Integer totalItems;
    /** 全部成员完成项次合计 */
    private Integer totalCompletions;
    /** 总进度百分比 0-100，无绑定项时为 null */
    private Integer progressPercent;
}
