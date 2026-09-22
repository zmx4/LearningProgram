package com.tick.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Account;
import com.tick.mapper.AccountMapper;
import com.tick.service.AccountService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = this.findAccountByUsernameOrEmail(username);

        if(account == null){
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        return User.withUsername(username)
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }
    public Account findAccountByUsernameOrEmail(String text){
        return this.query()
                .eq("username", text).or()
                .eq("email", text)
                .one();

    }
}
