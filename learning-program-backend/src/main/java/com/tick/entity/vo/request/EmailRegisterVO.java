package com.tick.entity.vo.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 注册与重置密码请求体。开启邮箱验证时 code 必填。
 */
@Data
public class EmailRegisterVO {
    @Email
    String email;
    @Length(max = 6, min = 6)
    String code;
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]+$")
    @Length(min = 1, max = 10)
    String username;
    /**
     * 密码强度由 PasswordPolicy 责任链统一校验（长度、大小写、数字、特殊字符等），
     * 这里不再声明约束，避免两处规则不一致时给出互相矛盾的提示。
     */
    String password;
}
