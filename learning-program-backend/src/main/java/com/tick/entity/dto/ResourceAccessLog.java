package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源访问流水，对应 db_resource_access_log。下载接口落记录，
 * 用于 resource 类型任务项的完成判定（任务发布后访问过即算完成）。
 */
@Data
@TableName("db_resource_access_log")
public class ResourceAccessLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("account_id")
    private Integer accountId;
    @TableField("resource_id")
    private Integer resourceId;
    @TableField("accessed_at")
    private LocalDateTime accessedAt;
}
