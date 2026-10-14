package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习小组成员，对应 db_study_group_member。组长同样占一行（role = owner），
 * 一个人可以加入或创建多个小组。
 */
@Data
@TableName("db_study_group_member")
public class StudyGroupMember {
    public static final String ROLE_OWNER = "owner";
    public static final String ROLE_MEMBER = "member";

    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("group_id")
    private Integer groupId;
    @TableField("account_id")
    private Integer accountId;
    private String role;
    @TableField("joined_at")
    private LocalDateTime joinedAt;
}
