package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文章下的一条评论。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionCommentVO {
    private Integer id;
    private Integer postId;
    private Integer authorId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;
}
