package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 * 站内通知，对应 db_notification。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("account_id")
    private Integer accountId;
    private String title;
    private String content;
    private String type;
    @TableField("is_read")
    @JsonProperty("read")
    private Boolean readStatus;
    @TableField("created_at")
    private Date createdAt;
}
