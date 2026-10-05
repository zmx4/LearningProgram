package com.tick.security.password;

import java.util.function.Predicate;

/**
 * 必需字符类别。把「怎么算命中」抽成策略，{@link com.tick.security.password.rule.RequiredCharacterRule}
 * 只负责「至少出现一次」这件事，四类字符因此共用同一个规则类。
 */
public enum CharacterClass {

    UPPERCASE("uppercase", "大写字母", Character::isUpperCase),
    LOWERCASE("lowercase", "小写字母", Character::isLowerCase),
    DIGIT("digit", "数字", Character::isDigit),
    /**
     * 特殊字符：既不是字母也不是数字、且不是空白。按 Unicode 判断，
     * 所以 € 这类符号同样算特殊字符，不局限于生成器用的那组 ASCII 符号。
     */
    SPECIAL("special", "特殊字符",
            character -> !Character.isLetterOrDigit(character) && !Character.isWhitespace(character));

    private final String code;
    private final String label;
    private final Predicate<Character> matcher;

    CharacterClass(String code, String label, Predicate<Character> matcher) {
        this.code = code;
        this.label = label;
        this.matcher = matcher;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public boolean matches(char character) {
        return matcher.test(character);
    }
}
