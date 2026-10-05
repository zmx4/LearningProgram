package com.tick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tick.entity.dto.DiscussionPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DiscussionPostMapper extends BaseMapper<DiscussionPost> {
    /**
     * 按发表时间倒序分页取文章。项目未引入 mybatis-plus-jsqlparser，
     * 所以这里直接用参数化的 LIMIT/OFFSET，而不是 selectPage。
     */
    @Select("SELECT id, account_id, title, content, comment_count, created_at "
            + "FROM db_discussion_post "
            + "ORDER BY created_at DESC, id DESC "
            + "LIMIT #{size} OFFSET #{offset}")
    List<DiscussionPost> selectPageByCreatedDesc(@Param("offset") long offset, @Param("size") int size);
}
