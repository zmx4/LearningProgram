package com.tick.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求体（已登录用户）。
 */
@Data
public class ChangePasswordVO {
    @NotBlank(message = "当前密码不能为空")
    private String oldPassword;

    /**
     * 新密码强度由 PasswordPolicy 责任链统一校验，这里不再声明长度约束。
     */
    private String newPassword;
}
