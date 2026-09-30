package com.tick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tick.entity.dto.AccountPoints;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AccountPointsMapper extends BaseMapper<AccountPoints> {
    @Select("SELECT total_points FROM db_account_points WHERE account_id = #{accountId} LIMIT 1")
    Integer findTotalPoints(Integer accountId);

    @Insert("""
            INSERT INTO db_account_points (account_id, total_points)
            VALUES (#{accountId}, #{points})
            ON DUPLICATE KEY UPDATE total_points = total_points + #{points}
            """)
    int addPoints(Integer accountId, Integer points);
}
