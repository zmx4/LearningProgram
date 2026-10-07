package com.tick.entity.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 排行榜响应：榜单条目 + 当前用户自己的名次。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardVO {
    /** 维度标识，如 points / study / check-in */
    private String metric;
    /** 维度名称，如「积分」 */
    private String metricLabel;
    /** 数值单位，如「分」；学习时长是「秒」 */
    private String unit;
    /** 本次返回的榜单条数上限 */
    private int limit;
    /** 参与该维度排名的用户总数 */
    private int rankedUsers;
    private List<LeaderboardEntryVO> entries;
    /** 当前用户的名次；在这个维度上还没有任何记录时为 null */
    private LeaderboardEntryVO me;
}
