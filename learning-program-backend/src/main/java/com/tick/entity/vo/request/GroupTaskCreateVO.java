package com.tick.entity.vo.request;

import lombok.Data;

/**
 * 组长发布学习任务的请求。任务作为 type = task 的小组消息展示在聊天区中。
 */
@Data
public class GroupTaskCreateVO {
    private String title;
    private String content;
}
