package com.tick.entity.vo.request;

import lombok.Data;

import java.util.List;

/**
 * 新增或修改课程的请求体。修改时 chapters 为该课程完整的章节列表（全量覆盖，
 * 带已有章节 id 的项原位更新以保留学员进度），questionIds 为课程关联题集的完整列表。
 */
@Data
public class CourseSaveVO {
    private String title;
    private String description;
    private String icon;
    private Integer sortOrder;
    /** 学员完成全部章节后一次性发放的奖励积分，null 视为 0 */
    private Integer rewardPoints;
    private List<ChapterItem> chapters;
    private List<Integer> questionSetIds;

    @Data
    public static class ChapterItem {
        private Integer id;
        private String title;
        private String content;
    }
}
