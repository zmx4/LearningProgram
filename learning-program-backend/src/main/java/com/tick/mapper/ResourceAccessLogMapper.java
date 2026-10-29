package com.tick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tick.entity.dto.ResourceAccessLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ResourceAccessLogMapper extends BaseMapper<ResourceAccessLog> {
    /**
     * 记录访问：每个 (account_id, resource_id) 只保留一行，重复访问刷新时间。
     * 任务完成判定只关心「最后一次访问」，这样流水表不会随点击次无限增长。
     */
    @Insert("INSERT INTO db_resource_access_log (account_id, resource_id, accessed_at) "
            + "VALUES (#{accountId}, #{resourceId}, #{accessedAt}) "
            + "ON DUPLICATE KEY UPDATE accessed_at = VALUES(accessed_at)")
    int upsertAccess(@Param("accountId") Integer accountId,
                     @Param("resourceId") Integer resourceId,
                     @Param("accessedAt") LocalDateTime accessedAt);
}
