package com.tick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tick.entity.dto.Cet6Word;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface Cet6WordMapper extends BaseMapper<Cet6Word> {
    @Select("SELECT id, word, translation FROM db_cet6 ORDER BY RAND() LIMIT #{count}")
    List<Cet6Word> selectRandom(int count);
}
