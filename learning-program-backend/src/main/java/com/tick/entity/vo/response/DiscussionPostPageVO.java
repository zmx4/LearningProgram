package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文章列表的分页结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionPostPageVO {
    private List<DiscussionPostSummaryVO> items;
    private long total;
    private int page;
    private int size;
}
