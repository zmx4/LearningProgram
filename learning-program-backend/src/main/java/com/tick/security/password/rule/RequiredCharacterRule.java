package com.tick.security.password.rule;

import com.tick.security.password.CharacterClass;
import com.tick.security.password.PasswordContext;
import com.tick.security.password.PasswordRule;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 必需字符规则：密码中至少要出现一个指定类别的字符（大写 / 小写 / 数字 / 特殊字符）。
 */
public final class RequiredCharacterRule implements PasswordRule {

    private final CharacterClass characterClass;

    public RequiredCharacterRule(CharacterClass characterClass) {
        this.characterClass = characterClass;
    }

    public CharacterClass getCharacterClass() {
        return characterClass;
    }

    @Override
    public Optional<String> check(PasswordContext context) {
        String password = context.rawPassword();
        if (password.isEmpty()) {
            // 空密码只由长度规则报「密码不能为空」，这里不再补一堆无意义的类别缺失
            return Optional.empty();
        }
        for (int index = 0; index < password.length(); index++) {
            if (characterClass.matches(password.charAt(index))) {
                return Optional.empty();
            }
        }
        return Optional.of(describe());
    }

    @Override
    public String code() {
        return "require-" + characterClass.getCode();
    }

    @Override
    public String describe() {
        return "至少包含一个" + characterClass.getLabel();
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("characterClass", characterClass.getCode());
        return parameters;
    }
}
