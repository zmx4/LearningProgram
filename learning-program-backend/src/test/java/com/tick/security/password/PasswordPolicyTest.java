package com.tick.security.password;

import com.tick.security.password.rule.AccountInfoRule;
import com.tick.security.password.rule.CommonPasswordRule;
import com.tick.security.password.rule.LengthRule;
import com.tick.security.password.rule.RequiredCharacterRule;
import com.tick.security.password.rule.WhitespaceRule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    private static PasswordPolicy policy(PasswordRule... rules) {
        return new PasswordPolicy(PasswordRuleChain.of(rules));
    }

    /** 与 application.yaml 默认值一致的完整策略。 */
    private static PasswordPolicy fullPolicy() {
        return policy(
                new LengthRule(8, 32),
                new RequiredCharacterRule(CharacterClass.UPPERCASE),
                new RequiredCharacterRule(CharacterClass.LOWERCASE),
                new RequiredCharacterRule(CharacterClass.DIGIT),
                new RequiredCharacterRule(CharacterClass.SPECIAL),
                new WhitespaceRule(),
                new AccountInfoRule(),
                new CommonPasswordRule(Set.of("password", "123456")));
    }

    private static boolean mentions(List<String> violations, String fragment) {
        return violations.stream().anyMatch(violation -> violation.contains(fragment));
    }

    @Test
    void acceptsPasswordMeetingEveryRule() {
        assertTrue(fullPolicy().violations("Abcd1234!", "tick", "tick@example.com").isEmpty());
    }

    @Test
    void reportsEveryBrokenRuleAtOnce() {
        // "123456"：太短、无大写、无小写、无特殊字符，而且是常见弱密码
        List<String> violations = fullPolicy().violations("123456", null, null);

        assertTrue(mentions(violations, "长度需在 8 到 32"), violations.toString());
        assertTrue(mentions(violations, "至少包含一个大写字母"), violations.toString());
        assertTrue(mentions(violations, "至少包含一个小写字母"), violations.toString());
        assertTrue(mentions(violations, "至少包含一个特殊字符"), violations.toString());
        assertTrue(mentions(violations, "常见弱密码"), violations.toString());
        // 链会走完而不是命中第一个就返回，数字规则是通过的，所以恰好 5 条
        assertEquals(5, violations.size(), violations.toString());
    }

    @Test
    void rejectsPasswordLongerThanMaximum() {
        List<String> violations = fullPolicy().violations("Aa1!" + "x".repeat(32), null, null);

        assertTrue(mentions(violations, "长度需在 8 到 32"), violations.toString());
    }

    @Test
    void rejectsEmptyAndNullPassword() {
        assertEquals(List.of("密码不能为空"), fullPolicy().violations(null, null, null));
        assertEquals(List.of("密码不能为空"), fullPolicy().violations("", null, null));
    }

    @Test
    void rejectsWhitespace() {
        List<String> violations = fullPolicy().violations("Abcd 1234!", null, null);

        assertTrue(mentions(violations, "空白字符"), violations.toString());
    }

    @Test
    void rejectsPasswordContainingUsername() {
        List<String> violations = fullPolicy().violations("Tick1234!a", "tick", null);

        assertTrue(mentions(violations, "用户名"), violations.toString());
    }

    @Test
    void rejectsPasswordContainingEmailOrItsLocalPart() {
        assertTrue(mentions(fullPolicy().violations("Ab1!someone@example.com", null, "someone@example.com"), "邮箱"));
        assertTrue(mentions(fullPolicy().violations("Ab1!sOMEONE", null, "someone@example.com"), "邮箱"));
    }

    @Test
    void ignoresVeryShortAccountInfo() {
        // 用户名只有 1 个字符时不参与判断，否则会误伤大量正常密码
        assertTrue(fullPolicy().violations("Abcd1234!", "a", null).isEmpty());
    }

    @Test
    void commonPasswordMatchIgnoresCase() {
        // 词表里是 "password"，大小写变形同样要拦住（精确匹配，不做包含判断）
        List<String> violations = fullPolicy().violations("PassWord", null, null);

        assertTrue(mentions(violations, "常见弱密码"), violations.toString());
        assertTrue(fullPolicy().violations("PassWord1!", null, null).stream()
                .noneMatch(v -> v.contains("常见弱密码")), "加了后缀就不再是精确匹配的弱密码");
    }

    @Test
    void messageJoinsAllViolations() {
        String message = fullPolicy().violationMessage("abc", null, null).orElseThrow();

        assertTrue(message.contains("；"), "多个问题应当汇总成一句：" + message);
    }

    @Test
    void describeExposesRulesForFrontend() {
        Map<String, Object> describe = fullPolicy().describe();

        assertEquals(8, describe.get("minLength"));
        assertEquals(32, describe.get("maxLength"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> requirements = (List<Map<String, Object>>) describe.get("requirements");
        assertEquals(8, requirements.size());
        assertEquals("length", requirements.get(0).get("code"));
        assertEquals(8, requirements.get(0).get("min"));
        assertEquals(32, requirements.get(0).get("max"));
        assertTrue(requirements.stream().anyMatch(r -> "require-uppercase".equals(r.get("code"))));
        assertTrue(requirements.stream().anyMatch(r -> "no-account-info".equals(r.get("code"))));
    }

    @Test
    void rulesCanBeDisabledByLeavingThemOutOfTheChain() {
        // 只装配长度规则：弱密码、缺大写都不再被拦，说明增删规则只影响装配处
        PasswordPolicy onlyLength = policy(new LengthRule(8, 32));

        assertTrue(onlyLength.violations("password", null, null).isEmpty());
        assertEquals(1, onlyLength.rules().size());
    }

    @Test
    void invalidLengthRangeIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new LengthRule(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new LengthRule(10, 5));
    }

    @Test
    void samePolicyServesEveryCaller() {
        PasswordPolicy shared = fullPolicy();

        assertFalse(shared.isValid("abc", "tick", "tick@example.com"));
        assertTrue(shared.isValid("Zx9!mNq2", "tick", "tick@example.com"));
    }
}
