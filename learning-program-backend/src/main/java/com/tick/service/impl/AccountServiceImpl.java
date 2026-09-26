package com.tick.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.EmailRegisterVO;
import com.tick.mapper.AccountMapper;
import com.tick.service.AccountService;
import jakarta.annotation.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.sql.Wrapper;
import java.util.Date;

@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    @Value("${spring.security.email}")
    boolean enabledEmailVerification;

    @Resource
    PasswordEncoder encoder;

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
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

    @Override
    public String registerEmailAccount(EmailRegisterVO vo) {
        String email = vo.getEmail();
        String username = vo.getUsername();
        if(this.existsAccountByEmail(email))return "此email已被其他用户注册.";
        if(enabledEmailVerification){
            return null;
        }
        String password = encoder.encode(vo.getPassword());
        Account account = new Account(null,username,password, email,"user",new Date());
        if(this.save(account)){
            return null;
        }else{
            return "内部错误,请联系管理员";
        }
    }

    private boolean existsAccountByEmail(String email){
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email",email));
    }
}
