package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小组成员最近一次访问小组详情页的时间，对应 db_group_member_visit。
 * 用于 member_visit 类型任务项的完成判定（任务发布后访问过小组即算完成）。
 */
@Data
@TableName("db_group_member_visit")
public class GroupMemberVisit {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("group_id")
    private Integer groupId;
    @TableField("account_id")
    private Integer accountId;
    @TableField("last_visit_at")
    private LocalDateTime lastVisitAt;
}
