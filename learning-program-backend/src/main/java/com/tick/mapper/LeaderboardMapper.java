package com.tick.mapper;

import com.tick.entity.vo.response.LeaderboardEntryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 排行榜查询。
 * <p>
 * 三个维度共用同一段「套壳」SQL（见下面两个常量）：内层用 ROW_NUMBER() 算名次，
 * 外层一次把「前 N 名」和「当前用户自己的那一行」都取回来 —— 无论用户排在第几。
 * 这样每个维度只需要提供一句求累计值的子查询，不必再单独写一条「我的名次」统计。
 * <p>
 * 学习时长按流水求和；积分与签到分别取总账与记录数。都是累计口径（总榜）。
 */
@Mapper
public interface LeaderboardMapper {

    /**
     * 套壳前半段：把各维度的累计值聚成 (account_id, metricValue)，再排上名次。
     * 用 COUNT(*) OVER () 顺带带回参与人数，省掉一次统计查询。
     */
    String RANKED_PREFIX =
            "<script>"
                    + "SELECT ranked.accountId AS accountId, ranked.username AS username, "
                    + "ranked.metricValue AS value, ranked.ranking AS rank, ranked.rankedUsers AS rankedUsers "
                    + "FROM ("
                    + "  SELECT a.id AS accountId, a.username AS username, t.metricValue AS metricValue, "
                    + "         ROW_NUMBER() OVER (ORDER BY t.metricValue DESC, t.account_id ASC) AS ranking, "
                    + "         COUNT(*) OVER () AS rankedUsers "
                    + "  FROM (";

    /** 套壳后半段：只保留前 N 名与当前用户本人，并按名次排序。 */
    String RANKED_SUFFIX =
            "  ) t JOIN db_account a ON a.id = t.account_id"
                    + ") ranked "
                    + "WHERE ranked.ranking &lt;= #{limit} OR ranked.accountId = #{accountId} "
                    + "ORDER BY ranked.ranking"
                    + "</script>";

    @Select(RANKED_PREFIX + "SELECT account_id, total_points AS metricValue FROM db_account_points" + RANKED_SUFFIX)
    List<LeaderboardEntryVO> selectPointsRanking(@Param("accountId") Integer accountId, @Param("limit") int limit);

    @Select(RANKED_PREFIX
            + "SELECT account_id, SUM(duration_seconds) AS metricValue FROM db_course_study_log GROUP BY account_id"
            + RANKED_SUFFIX)
    List<LeaderboardEntryVO> selectStudyRanking(@Param("accountId") Integer accountId, @Param("limit") int limit);

    @Select(RANKED_PREFIX + "SELECT account_id, COUNT(*) AS metricValue FROM db_check_in GROUP BY account_id" + RANKED_SUFFIX)
    List<LeaderboardEntryVO> selectCheckInRanking(@Param("accountId") Integer accountId, @Param("limit") int limit);
}
