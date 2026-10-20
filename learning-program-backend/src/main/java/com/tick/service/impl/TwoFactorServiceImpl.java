package com.tick.service.impl;

import com.tick.entity.dto.Account;
import com.tick.entity.vo.response.TotpSetupVO;
import com.tick.mail.EmailCodeService;
import com.tick.mail.MailType;
import com.tick.mapper.AccountMapper;
import com.tick.service.TotpService;
import com.tick.service.TwoFactorService;
import com.tick.utils.Const;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class TwoFactorServiceImpl implements TwoFactorService {
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private final AccountMapper accountMapper;
    private final TotpService totpService;
    private final EmailCodeService emailCodeService;
    private final StringRedisTemplate redis;
    private final boolean emailVerificationEnabled;

    public TwoFactorServiceImpl(AccountMapper accountMapper, TotpService totpService,
                                EmailCodeService emailCodeService, StringRedisTemplate redis,
                                @Value("${spring.security.email.enable}") boolean emailVerificationEnabled) {
        this.accountMapper = accountMapper;
        this.totpService = totpService;
        this.emailCodeService = emailCodeService;
        this.redis = redis;
        this.emailVerificationEnabled = emailVerificationEnabled;
    }

    @Override
    public TotpSetupVO setup(Integer accountId, String emailCode) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) return null;
        if (emailVerificationEnabled) {
            String error = emailCodeService.verifyAndConsume(account.getEmail(), MailType.TWO_FACTOR, emailCode);
            if (error != null) throw new IllegalArgumentException(error);
        }
        String secret = totpService.createSecret();
        account.setTotpSecret(secret);
        account.setTotpEnabled(false);
        accountMapper.updateById(account);
        return new TotpSetupVO(secret, totpService.getQrCodeUrl(account.getEmail(), "LearningProgram", secret));
    }

    @Override
    public String enable(Integer accountId, int code) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) return "登录状态无效";
        if (account.getTotpSecret() == null || !totpService.verifyCode(account.getTotpSecret(), code)) {
            return "两步验证码错误";
        }
        account.setTotpEnabled(true);
        return accountMapper.updateById(account) > 0 ? null : "内部错误,请联系管理员";
    }

    @Override
    public String disable(Integer accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) return "登录状态无效";
        account.setTotpEnabled(false);
        account.setTotpSecret(null);
        return accountMapper.updateById(account) > 0 ? null : "内部错误,请联系管理员";
    }

    @Override
    public boolean isEnabled(Account account) {
        return account != null && Boolean.TRUE.equals(account.getTotpEnabled())
                && account.getTotpSecret() != null && !account.getTotpSecret().isBlank();
    }

    @Override
    public String createLoginChallenge(Integer accountId) {
        String challenge = UUID.randomUUID().toString();
        redis.opsForValue().set(Const.TOTP_LOGIN_CHALLENGE + challenge, String.valueOf(accountId), CHALLENGE_TTL);
        return challenge;
    }

    @Override
    public Account verifyLoginChallenge(String challenge, int code) {
        String key = Const.TOTP_LOGIN_CHALLENGE + challenge;
        String accountId = redis.opsForValue().get(key);
        if (accountId == null) return null;
        Account account = accountMapper.selectById(Integer.valueOf(accountId));
        if (account == null || !isEnabled(account) || !totpService.verifyCode(account.getTotpSecret(), code)) {
            return null;
        }
        redis.delete(key);
        return account;
    }
}
