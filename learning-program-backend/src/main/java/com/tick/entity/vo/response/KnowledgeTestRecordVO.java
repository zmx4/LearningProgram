package com.tick.entity.vo.response;

import com.tick.entity.dto.KnowledgeTestAnswer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 返回给前端的知识测试记录，detail 由数据库中的 JSON 解析而来。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeTestRecordVO {
    private Integer id;
    private Integer typeId;
    private String typeName;
    private Integer totalCount;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer score;
    private Integer durationSeconds;
    private List<KnowledgeTestAnswer> detail;
    private LocalDateTime createdAt;
}
