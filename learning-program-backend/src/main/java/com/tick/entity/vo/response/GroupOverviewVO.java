package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 首页“我的小组”概览。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupOverviewVO {
    private List<GroupOverviewItemVO> groups;
    private List<GroupTodoVO> todos;
    private Integer totalOngoing;
    private Integer totalOverdue;
    private Integer totalCompleted;
}
