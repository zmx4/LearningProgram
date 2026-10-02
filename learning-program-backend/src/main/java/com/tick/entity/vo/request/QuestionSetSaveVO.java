package com.tick.entity.vo.request;

import lombok.Data;

import java.util.List;

/**
 * 新增或修改题集的请求体。修改时 questionIds 为该题集完整的题目列表（全量覆盖）。
 */
@Data
public class QuestionSetSaveVO {
    private String title;
    private String description;
    private List<Integer> questionIds;
}
