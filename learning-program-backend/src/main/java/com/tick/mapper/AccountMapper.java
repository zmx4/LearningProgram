package com.tick.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tick.entity.dto.Account;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<Account> {
}
