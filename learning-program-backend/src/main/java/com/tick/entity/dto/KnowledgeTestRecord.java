package com.tick.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识测试成绩记录。答题明细以 JSON 存放在 detail 列，结构对应 {@link KnowledgeTestAnswer}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("db_knowledge_test_record")
public class KnowledgeTestRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField("account_id")
    private Integer accountId;
    @TableField("type_id")
    private Integer typeId;
    @TableField("type_name")
    private String typeName;
    @TableField("total_count")
    private Integer totalCount;
    @TableField("correct_count")
    private Integer correctCount;
    @TableField("wrong_count")
    private Integer wrongCount;
    private Integer score;
    @TableField("duration_seconds")
    private Integer durationSeconds;
    private String detail;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
