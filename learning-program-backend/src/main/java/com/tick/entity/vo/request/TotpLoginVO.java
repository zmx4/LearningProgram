package com.tick.entity.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TotpLoginVO {
    @NotBlank(message = "登录挑战不能为空")
    private String challenge;
    @Pattern(regexp = "\\d{6}", message = "验证码必须是6位数字")
    private String code;
}
