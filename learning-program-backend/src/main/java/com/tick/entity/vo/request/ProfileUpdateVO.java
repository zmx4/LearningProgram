package com.tick.entity.vo.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人资料更新请求体。
 */
@Data
public class ProfileUpdateVO {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称不能超过20个字符")
    private String username;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱不能超过100个字符")
    private String email;

    @Size(max = 30, message = "手机号不能超过30个字符")
    private String phone;

    @Size(max = 120, message = "个人简介不能超过120个字符")
    private String bio;
}
