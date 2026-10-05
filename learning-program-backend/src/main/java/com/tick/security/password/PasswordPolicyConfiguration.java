package com.tick.security.password;

import com.tick.security.password.rule.AccountInfoRule;
import com.tick.security.password.rule.CommonPasswordRule;
import com.tick.security.password.rule.LengthRule;
import com.tick.security.password.rule.RequiredCharacterRule;
import com.tick.security.password.rule.WhitespaceRule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 按配置把密码规则装配成一条责任链。
 * <p>
 * 这里是唯一知道「有哪些规则、顺序如何」的地方：改配置或增删规则都只动这里，
 * 账号服务与接口拿到的始终是一个现成的 {@link PasswordPolicy}。
 */
@Configuration
public class PasswordPolicyConfiguration {

    @Bean
    public PasswordPolicy passwordPolicy(PasswordPolicyProperties properties) {
        List<PasswordRule> rules = new ArrayList<>();

        // 先报长度与字符类别，用户最容易理解和修改
        rules.add(new LengthRule(properties.getMinLength(), properties.getMaxLength()));
        if (properties.isRequireUppercase()) {
            rules.add(new RequiredCharacterRule(CharacterClass.UPPERCASE));
        }
        if (properties.isRequireLowercase()) {
            rules.add(new RequiredCharacterRule(CharacterClass.LOWERCASE));
        }
        if (properties.isRequireDigit()) {
            rules.add(new RequiredCharacterRule(CharacterClass.DIGIT));
        }
        if (properties.isRequireSpecial()) {
            rules.add(new RequiredCharacterRule(CharacterClass.SPECIAL));
        }
        if (properties.isForbidWhitespace()) {
            rules.add(new WhitespaceRule());
        }
        if (properties.isForbidAccountInfo()) {
            rules.add(new AccountInfoRule());
        }
        if (properties.isRejectCommonPasswords()) {
            rules.add(new CommonPasswordRule(new LinkedHashSet<>(properties.getCommonPasswords())));
        }

        return new PasswordPolicy(PasswordRuleChain.of(rules));
    }

    @Bean
    public PasswordGenerator passwordGenerator(PasswordPolicyProperties properties, PasswordPolicy passwordPolicy) {
        return new PasswordGenerator(properties, passwordPolicy);
    }
}
