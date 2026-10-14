package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习小组消息，对应 db_study_group_message。
 * type = text 为普通消息（组长组员均可发布）；type = task 为学习任务（仅组长可发布），带 title。
 */
@Data
@TableName("db_study_group_message")
public class StudyGroupMessage {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_TASK = "task";

    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("group_id")
    private Integer groupId;
    @TableField("account_id")
    private Integer accountId;
    private String type;
    private String title;
    private String content;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
