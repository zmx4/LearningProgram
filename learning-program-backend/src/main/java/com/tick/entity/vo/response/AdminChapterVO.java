package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 课程章节的管理端视图，含用于排序的 sortOrder 与编辑所需的正文。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminChapterVO {
    private Integer id;
    private String title;
    private String content;
    private Integer sortOrder;
}
