package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("db_account_points")
public class AccountPoints {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("account_id")
    private Integer accountId;
    @TableField("total_points")
    private Integer totalPoints;
}
