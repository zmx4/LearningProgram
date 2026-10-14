package com.tick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tick.entity.dto.TestRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestRecordMapper extends BaseMapper<TestRecord> {
}
