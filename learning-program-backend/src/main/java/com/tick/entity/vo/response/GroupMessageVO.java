package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 小组聊天区消息。type = task 时 title 有值，表示组长发布的学习任务。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessageVO {
    private Integer id;
    private Integer accountId;
    private String username;
    private String role;
    private String type;
    private String title;
    private String content;
    private LocalDateTime createdAt;
}
