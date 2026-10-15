package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 组长查看的单个学习任务完成矩阵：任务项 × 成员。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupTaskProgressVO {
    private Integer taskId;
    private String title;
    private List<GroupTaskItemVO> items;
    private List<MemberTaskProgressVO> members;
}
