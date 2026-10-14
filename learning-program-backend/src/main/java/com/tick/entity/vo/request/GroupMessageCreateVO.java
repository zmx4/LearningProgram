package com.tick.entity.vo.request;

import lombok.Data;

/**
 * 发布小组普通消息的请求，组长组员均可使用。
 */
@Data
public class GroupMessageCreateVO {
    private String content;
}
