package com.tick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tick.entity.dto.Cet4Word;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface Cet4WordMapper extends BaseMapper<Cet4Word> {
    @Select("SELECT id, word, translation FROM db_cet4 ORDER BY RAND() LIMIT #{count}")
    List<Cet4Word> selectRandom(int count);
}
