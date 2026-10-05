package com.tick.security.password;

import com.tick.security.password.rule.LengthRule;

import java.util.ArrayList;
import java.util.List;

/**
 * 责任链：按注册顺序串起若干 {@link PasswordRule}，逐节点校验。
 * <p>
 * 与「命中即中断」的经典写法不同，这里会走完整条链并**收集所有未通过的原因**，
 * 这样用户一次就能看到全部问题，而不是改一个报一个。节点之间互不依赖、可增删换序。
 */
public final class PasswordRuleChain {

    private final List<PasswordRule> rules;

    private PasswordRuleChain(List<PasswordRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static PasswordRuleChain of(List<PasswordRule> rules) {
        return new PasswordRuleChain(rules);
    }

    public static PasswordRuleChain of(PasswordRule... rules) {
        return new PasswordRuleChain(List.of(rules));
    }

    /**
     * 执行整条链。
     *
     * @return 所有未通过规则的说明，全部通过时为空列表
     */
    public List<String> validate(PasswordContext context) {
        List<String> violations = new ArrayList<>();
        for (PasswordRule rule : rules) {
            rule.check(context).ifPresent(violations::add);
        }
        return violations;
    }

    /** 链上的全部规则，按校验顺序。 */
    public List<PasswordRule> rules() {
        return rules;
    }

    /** 链上第一条长度规则，策略里需要它的上下限对外暴露。 */
    public LengthRule lengthRule() {
        return rules.stream()
                .filter(LengthRule.class::isInstance)
                .map(LengthRule.class::cast)
                .findFirst()
                .orElse(null);
    }
}
