package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 每日签到记录，对应 db_check_in。(account_id, checkin_date) 唯一，一天一次。
 */
@Data
@TableName("db_check_in")
public class CheckInRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("account_id")
    private Integer accountId;
    @TableField("checkin_date")
    private LocalDate checkinDate;
    private Integer points;
    private Integer streak;
}
