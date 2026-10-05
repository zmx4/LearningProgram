package com.tick.security.password;

import java.util.Map;
import java.util.Optional;

/**
 * 密码规则，责任链上的一个节点：只负责校验密码的一个方面。
 * <p>
 * 新增规则时只需实现本接口并注册到 {@link PasswordPolicyConfiguration} 的链上，
 * 调用方（账号服务、接口）无需改动 —— 对扩展开放、对修改关闭。
 * <p>
 * 规则同时要能「说明自己」：{@link #describe()} 与 {@link #parameters()} 会随
 * GET /api/auth/password-policy 一起返回，前端据此展示要求并做即时校验，
 * 避免把规则文案重复写死在前后端两处。
 */
public interface PasswordRule {

    /**
     * 校验密码。
     *
     * @return 通过时返回 {@link Optional#empty()}，不通过时返回可直接展示给用户的原因
     */
    Optional<String> check(PasswordContext context);

    /** 规则标识，前端按它决定用哪段逻辑做即时校验，例如 {@code require-uppercase}。 */
    String code();

    /** 规则的简短说明，例如「至少包含一个大写字母」。 */
    String describe();

    /** 规则参数，例如长度规则的上下限；没有参数时返回空 Map。 */
    default Map<String, Object> parameters() {
        return Map.of();
    }
}
