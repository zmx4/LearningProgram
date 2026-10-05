package com.tick.entity.vo.request;

import lombok.Data;

/**
 * 发表文章的请求体。
 */
@Data
public class DiscussionPostCreateVO {
    private String title;
    private String content;
}
