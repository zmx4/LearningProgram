package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 首页跨小组待办项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupTodoVO {
    private String key;
    private Integer groupId;
    private String groupName;
    private Integer taskId;
    private String taskTitle;
    private Integer itemId;
    private String itemTitle;
    private LocalDateTime dueAt;
}
