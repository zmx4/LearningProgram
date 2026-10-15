package com.tick.entity.vo.request;

import lombok.Data;

import java.util.List;

/**
 * 组长发布学习任务的请求。任务作为 type = task 的小组消息展示在聊天区中；
 * items 为可选的绑定项（课程/资源/组员访问/纯文本），用于追踪组员完成情况。
 */
@Data
public class GroupTaskCreateVO {
    private String title;
    private String content;
    private List<GroupTaskItemCreateVO> items;
}
