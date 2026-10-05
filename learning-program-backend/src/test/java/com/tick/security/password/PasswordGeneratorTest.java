package com.tick.security.password;

import com.tick.security.password.rule.AccountInfoRule;
import com.tick.security.password.rule.CommonPasswordRule;
import com.tick.security.password.rule.LengthRule;
import com.tick.security.password.rule.RequiredCharacterRule;
import com.tick.security.password.rule.WhitespaceRule;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordGeneratorTest {

    private static PasswordPolicy policy(int min, int max) {
        return new PasswordPolicy(PasswordRuleChain.of(
                new LengthRule(min, max),
                new RequiredCharacterRule(CharacterClass.UPPERCASE),
                new RequiredCharacterRule(CharacterClass.LOWERCASE),
                new RequiredCharacterRule(CharacterClass.DIGIT),
                new RequiredCharacterRule(CharacterClass.SPECIAL),
                new WhitespaceRule(),
                new AccountInfoRule(),
                new CommonPasswordRule(Set.of("password", "123456"))));
    }

    private static PasswordGenerator generator(PasswordPolicy policy, int min, int max, int generatedLength) {
        PasswordPolicyProperties properties = new PasswordPolicyProperties();
        properties.setMinLength(min);
        properties.setMaxLength(max);
        properties.setGeneratedLength(generatedLength);
        return new PasswordGenerator(properties, policy);
    }

    @Test
    void generatedPasswordAlwaysSatisfiesThePolicy() {
        PasswordPolicy policy = policy(8, 32);
        PasswordGenerator generator = generator(policy, 8, 32, 12);

        for (int round = 0; round < 500; round++) {
            String password = generator.generate();
            assertTrue(policy.violations(password, null, null).isEmpty(),
                    "生成的密码必须满足策略：" + password);
        }
    }

    @Test
    void generatedPasswordHasRequestedLength() {
        assertEquals(12, generator(policy(8, 32), 8, 32, 12).generate().length());
    }

    @Test
    void generatedLengthNeverGoesBelowMinimum() {
        // 配置成 6 但下限是 8 时，应当按下限生成
        assertEquals(8, generator(policy(8, 32), 8, 32, 6).generate().length());
    }

    @Test
    void generatedLengthIsCappedByMaximum() {
        assertEquals(20, generator(policy(8, 20), 8, 20, 40).generate().length());
    }

    @Test
    void generatedPasswordsDifferBetweenCalls() {
        PasswordGenerator generator = generator(policy(8, 32), 8, 32, 12);

        assertNotEquals(generator.generate(), generator.generate());
    }

    @Test
    void impossibleConfigurationFailsLoudly() {
        // 四类字符都要，但最大长度只有 3 —— 生成不出合规密码，必须直接报错
        PasswordGenerator generator = generator(policy(1, 3), 1, 3, 3);

        IllegalStateException error = assertThrows(IllegalStateException.class, generator::generate);
        assertTrue(error.getMessage().contains("不足以容纳"), error.getMessage());
    }

    @Test
    void generatedPasswordAvoidsAmbiguousCharacters() {
        PasswordGenerator generator = generator(policy(8, 32), 8, 32, 20);

        for (int round = 0; round < 200; round++) {
            String password = generator.generate();
            for (char ambiguous : new char[]{'I', 'O', 'l', '0', '1'}) {
                assertTrue(password.indexOf(ambiguous) < 0,
                        "生成结果不应包含易混淆字符 " + ambiguous + "：" + password);
            }
        }
    }
}
