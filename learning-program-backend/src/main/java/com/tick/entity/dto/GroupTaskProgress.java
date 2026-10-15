package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 手动标记类任务项（text）的完成记录，对应 db_group_task_progress。
 * 自动判定类（course/resource/member_visit）不落此表，查询时实时计算。
 */
@Data
@TableName("db_group_task_progress")
public class GroupTaskProgress {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("item_id")
    private Integer itemId;
    @TableField("account_id")
    private Integer accountId;
    @TableField("completed_at")
    private LocalDateTime completedAt;
}
