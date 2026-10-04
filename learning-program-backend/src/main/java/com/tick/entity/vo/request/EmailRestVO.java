package com.tick.entity.vo.request;

import jakarta.validation.constraints.Email;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 重置密码请求体（携带邮箱验证码与新密码）。
 */
@Data
public class EmailRestVO  {
    @Email
    String email;
    @Length(min = 6,max = 6)
    String code;
    @Length(min = 5,max = 20)
    String password;
}

