package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程的管理端视图。列表接口不返回 chapters（仅 chapterCount），
 * 详情接口返回完整章节与关联题集 id 列表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCourseVO {
    private Integer id;
    private String title;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Integer rewardPoints;
    private LocalDateTime createdAt;
    private Integer chapterCount;
    private List<Integer> questionSetIds;
    private List<AdminChapterVO> chapters;
}
