package com.tick.entity.vo.request;

import com.tick.entity.dto.KnowledgeTestAnswer;
import lombok.Data;

import java.util.List;

/**
 * 前端提交的知识测试结果。得分、错误数由服务端计算。
 */
@Data
public class KnowledgeTestResultVO {
    private Integer typeId;
    private Integer totalCount;
    private Integer correctCount;
    private Integer durationSeconds;
    private List<KnowledgeTestAnswer> detail;
}
