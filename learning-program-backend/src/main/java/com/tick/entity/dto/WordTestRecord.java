package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("db_word_test_record")
public class WordTestRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("account_id")
    private Integer accountId;
    private String source;
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
