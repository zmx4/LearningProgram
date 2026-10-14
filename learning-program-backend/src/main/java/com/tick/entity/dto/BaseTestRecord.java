package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试成绩记录父类：单词测试与知识测试共用的统计字段。
 * 各类测试统一持久化到 db_test_record（见 {@link TestRecord}），score 按正确率计算。
 */
@Data
public class BaseTestRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("account_id")
    private Integer accountId;
    @TableField("total_count")
    private Integer totalCount;
    @TableField("correct_count")
    private Integer correctCount;
    @TableField("wrong_count")
    private Integer wrongCount;
    private Integer score;
    @TableField("duration_seconds")
    private Integer durationSeconds;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
