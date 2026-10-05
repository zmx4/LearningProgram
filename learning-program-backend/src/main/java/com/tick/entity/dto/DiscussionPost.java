package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 讨论区文章。comment_count 为冗余计数，由 DiscussionService 在增删评论时同步维护。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_discussion_post")
public class DiscussionPost {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("account_id")
    private Integer accountId;
    private String title;
    private String content;
    @TableField("comment_count")
    private Integer commentCount;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
