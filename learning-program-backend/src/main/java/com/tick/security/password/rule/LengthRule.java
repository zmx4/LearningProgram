package com.tick.security.password.rule;

import com.tick.security.password.PasswordContext;
import com.tick.security.password.PasswordRule;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 长度规则：密码不能为空，且长度必须落在 [min, max] 区间内。
 */
public final class LengthRule implements PasswordRule {

    private final int min;
    private final int max;

    public LengthRule(int min, int max) {
        if (min < 1 || max < min) {
            throw new IllegalArgumentException("密码长度区间配置不合法：" + min + " - " + max);
        }
        this.min = min;
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public Optional<String> check(PasswordContext context) {
        String password = context.rawPassword();
        if (password.isEmpty()) {
            return Optional.of("密码不能为空");
        }
        if (password.length() < min || password.length() > max) {
            return Optional.of(describe());
        }
        return Optional.empty();
    }

    @Override
    public String code() {
        return "length";
    }

    @Override
    public String describe() {
        return "长度需在 " + min + " 到 " + max + " 个字符之间";
    }

    @Override
    public Map<String, Object> parameters() {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("min", min);
        parameters.put("max", max);
        return parameters;
    }
}
