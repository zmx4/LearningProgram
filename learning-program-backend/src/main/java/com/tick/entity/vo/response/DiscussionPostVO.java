package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文章详情，含正文全文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionPostVO {
    private Integer id;
    private String title;
    private String content;
    private Integer authorId;
    private String authorName;
    private Integer commentCount;
    private LocalDateTime createdAt;
}
