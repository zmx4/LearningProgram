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
 * 讨论区文章下的评论，平铺结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_discussion_comment")
public class DiscussionComment {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("post_id")
    private Integer postId;
    @TableField("account_id")
    private Integer accountId;
    private String content;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
