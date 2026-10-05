package com.tick.security.password;

/**
 * 密码校验上下文：除密码本身外，还带上账号信息，
 * 供「密码不能包含用户名 / 邮箱」这类规则使用。
 *
 * @param password 待校验的明文密码，可为 null
 * @param username 账号用户名，可为 null
 * @param email    账号邮箱，可为 null
 */
public record PasswordContext(String password, String username, String email) {

    public static PasswordContext of(String password, String username, String email) {
        return new PasswordContext(password, username, email);
    }

    public static PasswordContext of(String password) {
        return new PasswordContext(password, null, null);
    }

    /** 密码原文，null 视为空串，规则里不必反复判空。 */
    public String rawPassword() {
        return password == null ? "" : password;
    }

    public boolean hasPassword() {
        return !rawPassword().isEmpty();
    }
}
