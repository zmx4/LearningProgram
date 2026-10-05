package com.tick.security.password;

import com.tick.security.password.rule.LengthRule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 密码策略：全站唯一的「设置密码」入口。
 * <p>
 * 内部持有一条 {@link PasswordRuleChain} 责任链，注册、重置、改密等所有写密码的地方
 * 都调用它，规则本身不需要知道调用方是谁。策略还能自我描述（{@link #describe()}），
 * 由 GET /api/auth/password-policy 提供给前端，避免规则文案在前后端各写一份。
 */
public class PasswordPolicy {

    private final PasswordRuleChain chain;

    public PasswordPolicy(PasswordRuleChain chain) {
        this.chain = chain;
    }

    /** 所有未通过的规则说明。 */
    public List<String> violations(String password, String username, String email) {
        return chain.validate(PasswordContext.of(password, username, email));
    }

    /**
     * 校验密码是否满足策略。
     *
     * @return 全部满足返回 {@link Optional#empty()}，否则返回汇总后可直接展示的提示
     */
    public Optional<String> violationMessage(String password, String username, String email) {
        List<String> violations = violations(password, username, email);
        return violations.isEmpty() ? Optional.empty() : Optional.of(String.join("；", violations));
    }

    public boolean isValid(String password, String username, String email) {
        return violations(password, username, email).isEmpty();
    }

    public List<PasswordRule> rules() {
        return chain.rules();
    }

    public int minLength() {
        LengthRule lengthRule = chain.lengthRule();
        return lengthRule == null ? 0 : lengthRule.getMin();
    }

    public int maxLength() {
        LengthRule lengthRule = chain.lengthRule();
        return lengthRule == null ? Integer.MAX_VALUE : lengthRule.getMax();
    }

    /**
     * 策略的对外描述，用于前端展示要求并做即时校验。
     */
    public Map<String, Object> describe() {
        List<Map<String, Object>> requirements = new ArrayList<>();
        for (PasswordRule rule : chain.rules()) {
            Map<String, Object> requirement = new LinkedHashMap<>();
            requirement.put("code", rule.code());
            requirement.put("message", rule.describe());
            requirement.putAll(rule.parameters());
            requirements.add(requirement);
        }

        Map<String, Object> description = new LinkedHashMap<>();
        description.put("minLength", minLength());
        description.put("maxLength", maxLength());
        description.put("requirements", requirements);
        return description;
    }
}
