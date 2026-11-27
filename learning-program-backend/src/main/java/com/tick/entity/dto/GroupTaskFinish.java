package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 小组成员整任务完成台账，对应 db_group_task_finish。
 */
@Data
@TableName("db_group_task_finish")
public class GroupTaskFinish {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("group_id")
    private Integer groupId;
    @TableField("task_id")
    private Integer taskId;
    @TableField("account_id")
    private Integer accountId;
    @TableField("completed_at")
    private LocalDateTime completedAt;
    @TableField("points_awarded")
    private Integer pointsAwarded;
}
