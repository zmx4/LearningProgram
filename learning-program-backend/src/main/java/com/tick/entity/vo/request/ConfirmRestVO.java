package com.tick.entity.vo.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 邮箱验证码校验请求体（重置密码的验证步骤）。
 */
@Data
@AllArgsConstructor
public class ConfirmRestVO {
    @Email
    String email;
    @Length(min = 6,max = 6)
    String code;
}
