package com.tick.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 修改密码请求体（已登录用户）。
 */
@Data
public class ChangePasswordVO {
    @NotBlank(message = "当前密码不能为空")
    private String oldPassword;

    @Length(min = 6, max = 20, message = "新密码长度需要在6到20个字符之间")
    private String newPassword;
}
