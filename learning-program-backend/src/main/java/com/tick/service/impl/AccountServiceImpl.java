package com.tick.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.ConfirmRestVO;
import com.tick.entity.vo.request.EmailRegisterVO;
import com.tick.mapper.AccountMapper;
import com.tick.service.AccountService;
import com.tick.service.NotificationService;
import jakarta.annotation.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private NotificationService notificationService;

    @Override
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        Account account = this.findAccountByUsernameOrEmail(username);

        if (account == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        return User.withUsername(username)
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }

    public Account findAccountByUsernameOrEmail(String text) {
        return this.query()
                .eq("username", text).or()
                .eq("email", text)
                .one();

    }

    @Override
    public String registerEmailAccount(EmailRegisterVO vo) {
        String email = vo.getEmail();
        String username = vo.getUsername();
        if (this.existsAccountByEmail(email)) return "此email已被其他用户注册.";
        if (enabledEmailVerification) {
            return null;
        }
        String password = encoder.encode(vo.getPassword());
        Account account = new Account(null, username, password, email, "user", new Date());
        if (this.save(account)) {
            notificationService.save(new com.tick.entity.dto.Notification(
                    null,
                    account.getId(),
                    "欢迎加入学习平台",
                    "你的账号已经创建成功，开始规划今天的学习内容吧。",
                    "system",
                    false,
                    new Date()
            ));
            return null;
        } else {
            return "内部错误,请联系管理员";
        }
    }

    @Override
    public String resetConfirm(ConfirmRestVO vo) {
        String email = vo.getEmail();
        if (enabledEmailVerification) {
            String code = stringRedisTemplate.opsForValue().get(email);
            if (code == null)
                return "请先获取验证码";
            if (!code.equals(vo.getCode())) return "验证码有误";
        }
        return null;
    }

    @Override
    public String restEmailAccountPassword(EmailRegisterVO vo) {
        String email = vo.getEmail();
        String verify = this.resetConfirm(new ConfirmRestVO(email, vo.getCode()));
        if (verify == null) return verify;
        String password = encoder.encode(vo.getPassword());
        boolean result = this.update().eq("email", email).set("password", password).update();
        if (result && enabledEmailVerification) {
            stringRedisTemplate.delete(email);
        }
        return null;
    }

    private boolean existsAccountByEmail(String email) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email", email));
    }
}
