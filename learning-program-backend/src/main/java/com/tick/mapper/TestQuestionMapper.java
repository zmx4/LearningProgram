package com.tick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tick.entity.dto.TestQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TestQuestionMapper extends BaseMapper<TestQuestion> {
    /**
     * 随机抽取某个测试类型下的若干题目，kind 为 null 时不限题型。
     */
    @Select("<script>"
            + "SELECT id, type_id, kind, content, created_at "
            + "FROM db_test_question "
            + "WHERE type_id = #{typeId} "
            + "<if test='kind != null'>AND kind = #{kind} </if>"
            + "ORDER BY RAND() LIMIT #{count}"
            + "</script>")
    List<TestQuestion> selectRandomByType(@Param("typeId") Integer typeId,
                                          @Param("kind") String kind,
                                          @Param("count") int count);
}
