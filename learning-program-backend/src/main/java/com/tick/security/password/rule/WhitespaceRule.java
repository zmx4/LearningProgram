package com.tick.security.password.rule;

import com.tick.security.password.PasswordContext;
import com.tick.security.password.PasswordRule;

import java.util.Optional;

/**
 * 空白字符规则：密码中不允许出现空格、制表符等空白字符
 * （不少系统会在复制粘贴时把首尾空格一起带进来，直接拒绝比静默裁剪更安全）。
 */
public final class WhitespaceRule implements PasswordRule {

    @Override
    public Optional<String> check(PasswordContext context) {
        String password = context.rawPassword();
        for (int index = 0; index < password.length(); index++) {
            if (Character.isWhitespace(password.charAt(index))) {
                return Optional.of(describe());
            }
        }
        return Optional.empty();
    }

    @Override
    public String code() {
        return "no-whitespace";
    }

    @Override
    public String describe() {
        return "不能包含空格等空白字符";
    }
}
