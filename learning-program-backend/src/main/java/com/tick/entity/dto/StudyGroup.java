package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习小组，对应 db_study_group。invite_code 为组长分享给组员的入群凭证。
 */
@Data
@TableName("db_study_group")
public class StudyGroup {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    @TableField("owner_id")
    private Integer ownerId;
    @TableField("invite_code")
    private String inviteCode;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
