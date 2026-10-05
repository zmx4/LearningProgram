package com.tick.security.password;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 随机密码生成器：按当前策略生成一个**必定满足策略**的密码，供管理员重置密码使用。
 * <p>
 * 生成方式是「先给每类必需字符各放一个，再用字母数字补齐到目标长度，最后整体打乱」，
 * 生成后会跑一遍策略做自检 —— 配置被改坏时立刻抛错，而不是把不合规的密码发给用户。
 * 字符集剔除了 I/O/l/0/1 这类易混淆字符，方便用户照着输入。
 */
public class PasswordGenerator {

    private static final String UPPERCASE_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWERCASE_POOL = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGIT_POOL = "23456789";
    private static final String DEFAULT_SPECIAL_POOL = "!@#$%^&*()-_=+[]{};:,.?/|~";

    private final PasswordPolicyProperties properties;
    private final PasswordPolicy policy;
    private final SecureRandom random = new SecureRandom();

    public PasswordGenerator(PasswordPolicyProperties properties, PasswordPolicy policy) {
        this.properties = properties;
        this.policy = policy;
    }

    public String generate() {
        List<String> pools = requiredPools();

        int length = Math.max(properties.getGeneratedLength(), properties.getMinLength());
        length = Math.min(length, properties.getMaxLength());
        if (length < pools.size()) {
            throw new IllegalStateException("密码策略配置不合法：最大长度 " + properties.getMaxLength()
                    + " 不足以容纳 " + pools.size() + " 类必需字符");
        }

        List<Character> characters = new ArrayList<>(length);
        for (String pool : pools) {
            characters.add(pick(pool));
        }
        String filler = String.join("", pools);
        while (characters.size() < length) {
            characters.add(pick(filler));
        }
        Collections.shuffle(characters, random);

        StringBuilder password = new StringBuilder(length);
        characters.forEach(password::append);
        String generated = password.toString();

        policy.violationMessage(generated, null, null).ifPresent(reason -> {
            throw new IllegalStateException("生成的随机密码不满足当前策略：" + reason);
        });
        return generated;
    }

    private List<String> requiredPools() {
        List<String> pools = new ArrayList<>();
        if (properties.isRequireUppercase()) {
            pools.add(UPPERCASE_POOL);
        }
        if (properties.isRequireLowercase()) {
            pools.add(LOWERCASE_POOL);
        }
        if (properties.isRequireDigit()) {
            pools.add(DIGIT_POOL);
        }
        if (properties.isRequireSpecial()) {
            String special = properties.getSpecialCharacters();
            pools.add(special == null || special.isEmpty() ? DEFAULT_SPECIAL_POOL : special);
        }
        if (pools.isEmpty()) {
            pools.add(LOWERCASE_POOL);
            pools.add(DIGIT_POOL);
        }
        return pools;
    }

    private char pick(String pool) {
        return pool.charAt(random.nextInt(pool.length()));
    }
}
