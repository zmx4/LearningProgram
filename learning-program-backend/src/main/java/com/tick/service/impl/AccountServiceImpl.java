package com.tick.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.tick.entity.dto.Account;
import com.tick.entity.vo.request.ChangePasswordVO;
import com.tick.entity.vo.request.ConfirmRestVO;
import com.tick.entity.vo.request.EmailRegisterVO;
import com.tick.entity.vo.request.ProfileUpdateVO;
import com.tick.mapper.AccountMapper;
import com.tick.security.password.PasswordGenerator;
import com.tick.security.password.PasswordPolicy;
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
import java.util.List;

/**
 * 账号服务实现。
 */
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
    @Resource
    private PasswordPolicy passwordPolicy;
    @Resource
    private PasswordGenerator passwordGenerator;

    /**
     * 统一的密码强度校验：所有写密码的入口都先过这里，规则本身见 PasswordPolicy。
     *
     * @return 通过返回 null，否则返回可直接展示给用户的提示
     */
    private String checkPasswordPolicy(String password, String username, String email) {
        return passwordPolicy.violationMessage(password, username, email).orElse(null);
    }

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
        List<Account> rows = this.query()
                .eq("username", text).or()
                .eq("email", text)
                .list();
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Override
    public String registerEmailAccount(EmailRegisterVO vo) {
        String email = vo.getEmail();
        String username = vo.getUsername();
        if (this.existsAccountByEmail(email)) return "此email已被其他用户注册.";
        if (enabledEmailVerification) {
            return null;
        }
        String policyMessage = checkPasswordPolicy(vo.getPassword(), username, email);
        if (policyMessage != null) return policyMessage;
        String password = encoder.encode(vo.getPassword());
        Account account = new Account(null, username, password, email, null, null, "user", new Date());
        if (this.save(account)) {
            notificationService.save(new com.tick.entity.dto.Notification(
                    null,
                    account.getId(),
                    "欢迎加入学习平台",
                    "你的账号已经创建成功，开始规划今天的学习内容吧。",
                    null,
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
    public String createAccount(String username, String email, String password) {
        if (this.existsAccountByEmail(email)) return "此email已被其他用户注册.";
        if (this.existsAccountByUsername(username)) return "此用户名已被其他用户注册.";
        String policyMessage = checkPasswordPolicy(password, username, email);
        if (policyMessage != null) return policyMessage;
        Account account = new Account(null, username, encoder.encode(password), email, null, null, "user", new Date());
        if (this.save(account)) {
            notificationService.save(new com.tick.entity.dto.Notification(
                    null,
                    account.getId(),
                    "欢迎加入学习平台",
                    "你的账号已经创建成功，开始规划今天的学习内容吧。",
                    null,
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
    public String changePassword(Integer accountId, ChangePasswordVO vo) {
        Account account = this.getById(accountId);
        if (account == null) return "登录状态无效";
        if (!encoder.matches(vo.getOldPassword(), account.getPassword())) return "当前密码不正确";
        if (encoder.matches(vo.getNewPassword(), account.getPassword())) return "新密码不能与当前密码相同";
        String policyMessage = checkPasswordPolicy(vo.getNewPassword(), account.getUsername(), account.getEmail());
        if (policyMessage != null) return policyMessage;
        account.setPassword(encoder.encode(vo.getNewPassword()));
        if (!this.updateById(account)) return "内部错误,请联系管理员";
        return null;
    }

    @Override
    public String resetPassword(Integer accountId) {
        Account account = this.getById(accountId);
        if (account == null) return null;
        // 由生成器保证新密码满足当前策略
        String newPassword = passwordGenerator.generate();
        account.setPassword(encoder.encode(newPassword));
        if (!this.updateById(account)) return null;
        notificationService.save(new com.tick.entity.dto.Notification(
                null,
                account.getId(),
                "登录密码已重置",
                "管理员重置了你的账号密码，请使用新密码登录后及时修改。",
                null,
                "system",
                false,
                new Date()
        ));
        return newPassword;
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
        String policyMessage = checkPasswordPolicy(vo.getPassword(), null, email);
        if (policyMessage != null) return policyMessage;
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

    private boolean existsAccountByUsername(String username) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("username", username));
    }

    @Override
    public Account updateProfile(Integer accountId, ProfileUpdateVO vo) {
        Account account = this.getById(accountId);
        if (account == null) {
            return null;
        }

        String email = normalize(vo.getEmail());
        boolean emailChanged = !emailEquals(email, account.getEmail());
        if (emailChanged && email != null
                && this.query().eq("email", email).ne("id", accountId).exists()) {
            throw new IllegalArgumentException("此邮箱已被其他用户使用");
        }
        account.setUsername(vo.getUsername().trim());
        account.setEmail(email);
        account.setPhone(normalize(vo.getPhone()));
        account.setBio(normalize(vo.getBio()));
        this.updateById(account);
        return account;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean emailEquals(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }
}
