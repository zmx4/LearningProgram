package com.tick.security.password;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 密码策略配置，对应 application.yaml 中的 learning.password.policy。
 * <p>
 * 每条规则是否启用、长度上下限、生成密码长度等都可以按环境调整，不必改代码。
 */
@Data
@ConfigurationProperties(prefix = "learning.password.policy")
public class PasswordPolicyProperties {

    /** 密码长度下限（含） */
    private int minLength = 8;

    /** 密码长度上限（含） */
    private int maxLength = 32;

    private boolean requireUppercase = true;
    private boolean requireLowercase = true;
    private boolean requireDigit = true;
    private boolean requireSpecial = true;

    /** 是否禁止空白字符 */
    private boolean forbidWhitespace = true;

    /** 是否禁止密码里出现用户名 / 邮箱 */
    private boolean forbidAccountInfo = true;

    /** 是否拒绝常见弱密码 */
    private boolean rejectCommonPasswords = true;

    /** 管理员重置密码时生成的随机密码长度 */
    private int generatedLength = 12;

    /** 生成随机密码时可用的特殊字符集合 */
    private String specialCharacters = "!@#$%^&*()-_=+[]{};:,.?/|~";

    /**
     * 弱密码词表（忽略大小写精确匹配）。
     * 默认取最常见的若干口令，可在配置里整体覆盖。
     */
    private List<String> commonPasswords = List.of(
            "123456", "12345678", "123456789", "1234567890", "111111", "123123", "abc123",
            "password", "password1", "password123", "passw0rd", "qwerty", "qwerty123",
            "admin", "admin123", "root", "letmein", "welcome", "iloveyou", "monkey",
            "dragon", "master", "sunshine", "a123456", "1qaz2wsx", "qazwsx");
}
