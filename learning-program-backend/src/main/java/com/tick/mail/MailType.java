package com.tick.mail;

/**
 * 邮箱验证码的用途。注册与重置使用**不同的 Redis 键**，
 * 避免注册验证码被拿去重置别人的密码。
 */
public enum MailType {

    REGISTER("register", "注册验证码"),
    RESET("reset", "重置密码验证码");

    private final String code;
    private final String label;

    MailType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static MailType fromCode(String code) {
        if (code != null) {
            String normalized = code.trim();
            for (MailType type : values()) {
                if (type.code.equalsIgnoreCase(normalized)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("不支持的验证码类型，可选值：" + codes());
    }

    public static String codes() {
        StringBuilder builder = new StringBuilder();
        for (MailType type : values()) {
            if (builder.length() > 0) {
                builder.append('、');
            }
            builder.append(type.code);
        }
        return builder.toString();
    }
}
