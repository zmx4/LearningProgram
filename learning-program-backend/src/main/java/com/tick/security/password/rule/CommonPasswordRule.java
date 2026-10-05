package com.tick.security.password.rule;

import com.tick.security.password.PasswordContext;
import com.tick.security.password.PasswordRule;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 弱密码规则：拒绝口令字典里最常见的那些密码（忽略大小写比较）。
 * 词表默认写在 {@link com.tick.security.password.PasswordPolicyProperties} 中，可在配置里覆盖。
 */
public final class CommonPasswordRule implements PasswordRule {

    private final Set<String> commonPasswords;

    public CommonPasswordRule(Set<String> commonPasswords) {
        Set<String> normalized = new LinkedHashSet<>();
        if (commonPasswords != null) {
            for (String candidate : commonPasswords) {
                if (candidate != null && !candidate.isBlank()) {
                    normalized.add(candidate.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        this.commonPasswords = Set.copyOf(normalized);
    }

    @Override
    public Optional<String> check(PasswordContext context) {
        if (!context.hasPassword()) {
            return Optional.empty();
        }
        String password = context.rawPassword().toLowerCase(Locale.ROOT);
        return commonPasswords.contains(password) ? Optional.of(describe()) : Optional.empty();
    }

    @Override
    public String code() {
        return "not-common";
    }

    @Override
    public String describe() {
        return "不能是常见弱密码";
    }
}
