package com.tick.entity.vo.request;

import lombok.Data;

/**
 * 发布学习任务时绑定的单个任务项。
 * type：course（refId 为课程 id）、resource（refId 为资源 id）、
 * member_visit（组员必须访问小组）、text（纯文本，组员手动标记完成）。
 */
@Data
public class GroupTaskItemCreateVO {
    private String type;
    private Integer refId;
    private String title;
}
