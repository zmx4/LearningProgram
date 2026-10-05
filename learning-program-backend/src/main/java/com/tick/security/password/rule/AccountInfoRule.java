package com.tick.security.password.rule;

import com.tick.security.password.PasswordContext;
import com.tick.security.password.PasswordRule;

import java.util.Locale;
import java.util.Optional;

/**
 * 账号信息规则：密码中不能包含用户名或邮箱（含邮箱 @ 前面的部分）。
 * 太短的账号信息（少于 {@value #MIN_TOKEN_LENGTH} 个字符）不参与判断，
 * 否则用户名形如「a」时会误伤大量正常密码。
 */
public final class AccountInfoRule implements PasswordRule {

    private static final int MIN_TOKEN_LENGTH = 3;

    @Override
    public Optional<String> check(PasswordContext context) {
        if (!context.hasPassword()) {
            return Optional.empty();
        }
        String password = context.rawPassword().toLowerCase(Locale.ROOT);

        if (contains(password, normalize(context.username()))) {
            return Optional.of("密码不能包含用户名");
        }
        String email = normalize(context.email());
        if (!email.isEmpty() && password.contains(email)) {
            return Optional.of("密码不能包含邮箱地址");
        }
        String localPart = localPartOf(email);
        if (contains(password, localPart)) {
            return Optional.of("密码不能包含邮箱地址");
        }
        return Optional.empty();
    }

    private String localPartOf(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private boolean contains(String password, String token) {
        return token.length() >= MIN_TOKEN_LENGTH && password.contains(token);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String code() {
        return "no-account-info";
    }

    @Override
    public String describe() {
        return "不能包含用户名或邮箱";
    }
}
