package com.tick.mail;

import com.tick.utils.Const;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务：生成、存储（Redis）、频控、校验。
 * <p>
 * 键按用途分开：{@code email::code:register:<email>} 与 {@code email::code:reset:<email>}，
 * 所以注册验证码不能拿来重置密码。验证码只在服务端与日志里存在，
 * **不会**出现在任何 HTTP 响应中。
 */
@Service
public class EmailCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final MailProperties properties;
    private final MailDispatcher mailDispatcher;

    public EmailCodeService(StringRedisTemplate redis, MailProperties properties, MailDispatcher mailDispatcher) {
        this.redis = redis;
        this.properties = properties;
        this.mailDispatcher = mailDispatcher;
    }

    public static String codeKey(MailType type, String email) {
        return Const.EMAIL_CODE + type.getCode() + ":" + normalize(email);
    }

    public static String intervalKey(MailType type, String email) {
        return Const.EMAIL_CODE_INTERVAL + type.getCode() + ":" + normalize(email);
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim();
    }

    /**
     * 生成并发送验证码。
     *
     * @return 成功返回 null，失败返回可直接展示给用户的原因
     */
    public String sendCode(String email, MailType type) {
        String target = normalize(email);
        if (target.isEmpty()) {
            return "请填写邮箱";
        }

        MailProperties.Code config = properties.getCode();
        String intervalKey = intervalKey(type, target);
        Boolean allowed = redis.opsForValue()
                .setIfAbsent(intervalKey, "1", config.getIntervalSeconds(), TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(allowed)) {
            return "验证码发送过于频繁，请 " + config.getIntervalSeconds() + " 秒后再试";
        }

        String code = generateCode(config.getLength());
        redis.opsForValue().set(codeKey(type, target), code, config.getTtlSeconds(), TimeUnit.SECONDS);

        String minutes = String.valueOf(Math.max(1, config.getTtlSeconds() / 60));
        String content = "你好：\n\n你正在进行" + type.getLabel() + "，验证码为：" + code
                + "\n验证码 " + minutes + " 分钟内有效，请勿泄露给他人。\n\n如果不是你本人操作，请忽略这封邮件。";
        String subject = "【学习平台】" + type.getLabel();

        // 投递永远不会抛异常（队列不可用会自动回退成直接发送/写日志）
        mailDispatcher.dispatch(new EmailMessage(target, subject, content, code));
        return null;
    }

    /**
     * 校验验证码，**不消费**（用于「下一步」这类预校验）。
     */
    public String verify(String email, MailType type, String code) {
        String stored = redis.opsForValue().get(codeKey(type, normalize(email)));
        if (stored == null) {
            return "请先获取验证码";
        }
        if (code == null || !stored.equals(code.trim())) {
            return "验证码有误";
        }
        return null;
    }

    /**
     * 校验并消费验证码（成功后立即失效，防止一个问题码反复使用）。
     */
    public String verifyAndConsume(String email, MailType type, String code) {
        String result = verify(email, type, code);
        if (result == null) {
            clear(email, type);
        }
        return result;
    }

    /** 使验证码失效（注册成功、重置成功之后调用）。 */
    public void clear(String email, MailType type) {
        redis.delete(codeKey(type, normalize(email)));
    }

    private String generateCode(int length) {
        int digits = Math.max(1, length);
        StringBuilder builder = new StringBuilder(digits);
        for (int index = 0; index < digits; index++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}
