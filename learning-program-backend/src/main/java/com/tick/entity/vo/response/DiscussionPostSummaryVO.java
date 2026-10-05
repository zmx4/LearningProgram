package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 列表中的文章摘要，不含正文全文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscussionPostSummaryVO {
    private Integer id;
    private String title;
    private String summary;
    private Integer authorId;
    private String authorName;
    private Integer commentCount;
    private LocalDateTime createdAt;
}
