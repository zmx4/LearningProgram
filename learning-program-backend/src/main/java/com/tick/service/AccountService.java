package com.tick.service;


import com.baomidou.mybatisplus.spring.service.IService;
import com.tick.entity.dto.Account;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface AccountService extends IService<Account> , UserDetailsService {
    public Account findAccountByUsernameOrEmail(String text);
}
