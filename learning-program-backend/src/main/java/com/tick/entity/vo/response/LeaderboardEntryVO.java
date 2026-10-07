package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 排行榜里的一行。既用于榜单条目，也用于返回「我的排名」。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryVO {
    /** 名次，从 1 开始；并列时按 account_id 升序错开 */
    private Integer rank;
    private Integer accountId;
    private String username;
    /** 该维度的累计值；学习时长单位是秒 */
    private Long value;
    /** 参与该维度排名的用户总数（每行都带，方便前端显示「共 N 人」） */
    private Integer rankedUsers;
}
