package com.tick.entity.vo.request;

import com.tick.entity.dto.TestQuestionContent;
import lombok.Data;

/**
 * 新增题目的请求体。content 的结构见 {@link TestQuestionContent}。
 */
@Data
public class TestQuestionCreateVO {
    private Integer typeId;
    private String kind;
    private TestQuestionContent content;
}
